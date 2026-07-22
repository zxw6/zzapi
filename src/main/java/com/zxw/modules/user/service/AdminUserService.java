package com.zxw.modules.user.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.apikey.service.ApiKeyAuthCacheService;
import com.zxw.modules.user.dto.UserCreateRequest;
import com.zxw.modules.user.dto.UserListItemResponse;
import com.zxw.modules.user.dto.UserUpdateRequest;
import com.zxw.modules.user.dto.WalletRechargeRequest;
import com.zxw.persistence.entity.TransactionEntity;
import com.zxw.persistence.entity.UserEntity;
import com.zxw.persistence.entity.WalletEntity;
import com.zxw.persistence.mapper.TransactionMapper;
import com.zxw.persistence.mapper.ApiKeyMapper;
import com.zxw.persistence.mapper.UserCleanupMapper;
import com.zxw.persistence.mapper.UserMapper;
import com.zxw.persistence.mapper.UserQueryMapper;
import com.zxw.persistence.mapper.WalletMapper;
import com.zxw.persistence.model.UserListView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
/**
 * 用户管理服务。
 * 负责用户资料维护、删除以及钱包充值等后台操作。
 */
public class AdminUserService {

    private final UserMapper userMapper;
    private final UserQueryMapper userQueryMapper;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final UserCleanupMapper userCleanupMapper;
    private final PasswordService passwordService;
    private final ApiKeyAuthCacheService apiKeyAuthCacheService;

    public AdminUserService(UserMapper userMapper,
                            UserQueryMapper userQueryMapper,
                            WalletMapper walletMapper,
                            TransactionMapper transactionMapper,
                            ApiKeyMapper apiKeyMapper,
                            UserCleanupMapper userCleanupMapper,
                            PasswordService passwordService,
                            ApiKeyAuthCacheService apiKeyAuthCacheService) {
        this.userMapper = userMapper;
        this.userQueryMapper = userQueryMapper;
        this.walletMapper = walletMapper;
        this.transactionMapper = transactionMapper;
        this.apiKeyMapper = apiKeyMapper;
        this.userCleanupMapper = userCleanupMapper;
        this.passwordService = passwordService;
        this.apiKeyAuthCacheService = apiKeyAuthCacheService;
    }

    /**
     * 查询用户列表。
     */
    public List<UserListItemResponse> listUsers() {
        // 普通用户只能查看自己，管理员可以查看全部用户
        JwtUser currentUser = AdminContext.require();
        if (!AdminContext.isAdmin()) {
            return List.of(getUser(currentUser.userId()));
        }

        return userQueryMapper.selectUsers().stream()
                .map(this::toUserListItemResponse)
                .toList();
    }

    /**
     * 查询单个用户详情。
     */
    public UserListItemResponse getUser(Long userId) {
        // 非管理员场景下忽略传入 userId，统一查询当前登录用户
        JwtUser currentUser = AdminContext.require();
        Long targetUserId = AdminContext.isAdmin() ? userId : currentUser.userId();

        UserListView user = userQueryMapper.selectUserById(targetUserId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        return toUserListItemResponse(user);
    }

    @Transactional
    /**
     * 创建新用户并初始化钱包。
     */
    public void createUser(UserCreateRequest request) {
        AdminContext.requireAdmin();
        // 创建前先检查用户名是否重复
        if (userMapper.existsActiveByUsername(request.username())) {
            throw new BusinessException("Username already exists");
        }

        // 先创建用户主记录
        String roleCode = request.roleCode() == null || request.roleCode().isBlank() ? "USER" : request.roleCode();
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordService.encode(request.password()));
        user.setNickname(blankToNull(request.nickname()));
        user.setEmail(blankToNull(request.email()));
        user.setPhone(blankToNull(request.phone()));
        user.setRoleCode(roleCode);
        user.setStatus("ACTIVE");
        user.setPackageRestrictionEnabled("ADMIN".equalsIgnoreCase(roleCode) ? 0 : 1);
        user.setMaxConcurrentRequests(normalizeConcurrencyLimit(request.maxConcurrentRequests()));
        user.setMaxConcurrentStreams(normalizeConcurrencyLimit(request.maxConcurrentStreams()));
        userMapper.insert(user);

        // 再初始化钱包
        BigDecimal initialBalance = request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance();
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(user.getId());
        wallet.setBalance(initialBalance);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setTotalRecharge(initialBalance);
        wallet.setTotalConsume(BigDecimal.ZERO);
        walletMapper.insert(wallet);

        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            // 如果配置了初始余额，则补一条充值流水
            TransactionEntity transaction = new TransactionEntity();
            transaction.setUserId(user.getId());
            transaction.setWalletId(wallet.getId());
            transaction.setOrderNo(buildOrderNo("R"));
            transaction.setTransactionType("RECHARGE");
            transaction.setDirection("IN");
            transaction.setAmount(initialBalance);
            transaction.setBalanceBefore(BigDecimal.ZERO);
            transaction.setBalanceAfter(initialBalance);
            transaction.setStatus("SUCCESS");
            transaction.setDescriptionText("Initial balance on user creation");
            transaction.setTransactionDate(LocalDate.now());
            transactionMapper.insert(transaction);
        }
    }

    @Transactional
    /**
     * 更新用户基础资料。
     */
    public void updateUser(Long userId, UserUpdateRequest request) {
        // 管理员可以更新任意用户，普通用户只能更新自己
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        Long targetUserId = admin ? userId : currentUser.userId();

        UserListItemResponse targetUser = getUser(targetUserId);
        String nextRoleCode = admin ? defaultIfBlank(request.roleCode(), targetUser.roleCode()) : targetUser.roleCode();
        String nextStatus = admin ? defaultIfBlank(request.status(), targetUser.status()) : targetUser.status();
        String nextNickname = fallbackBlank(request.nickname(), targetUser.nickname());
        String nextEmail = fallbackBlank(request.email(), targetUser.email());
        String nextPhone = fallbackBlank(request.phone(), targetUser.phone());

        // 邮箱变更时要检查是否和其他用户冲突
        if (nextEmail != null && !nextEmail.isBlank()
                && userMapper.existsActiveByEmailExcludingId(nextEmail, targetUserId)) {
            throw new BusinessException("Email is already used by another user");
        }

        // 普通用户不允许修改角色和状态
        if (!admin && request.roleCode() != null && !request.roleCode().isBlank()) {
            throw new BusinessException("Normal users cannot change role");
        }
        if (!admin && request.status() != null && !request.status().isBlank()) {
            throw new BusinessException("Normal users cannot change status");
        }
        if (!admin && request.maxConcurrentRequests() != null) {
            throw new BusinessException("Normal users cannot change concurrency limits");
        }
        if (!admin && request.maxConcurrentStreams() != null) {
            throw new BusinessException("Normal users cannot change concurrency limits");
        }

        // 按最终计算后的字段更新用户资料
        UserEntity updateUser = new UserEntity();
        updateUser.setNickname(blankToNull(nextNickname));
        updateUser.setEmail(blankToNull(nextEmail));
        updateUser.setPhone(blankToNull(nextPhone));
        updateUser.setRoleCode(nextRoleCode);
        updateUser.setStatus(nextStatus);
        updateUser.setUpdatedAt(LocalDateTime.now());
        if (admin) {
            if (request.maxConcurrentRequests() != null) {
                updateUser.setMaxConcurrentRequests(normalizeConcurrencyLimit(request.maxConcurrentRequests()));
            }
            if (request.maxConcurrentStreams() != null) {
                updateUser.setMaxConcurrentStreams(normalizeConcurrencyLimit(request.maxConcurrentStreams()));
            }
        }
        if (request.password() != null && !request.password().isBlank()) {
            updateUser.setPasswordHash(passwordService.encode(request.password()));
        }

        userMapper.updateActiveUser(targetUserId, updateUser);
        apiKeyAuthCacheService.evictByUserId(targetUserId);
    }

    /**
     * 单独更新用户状态。
     */
    public void updateStatus(Long userId, String status) {
        AdminContext.requireAdmin();
        // 单独更新用户启用状态
        int updated = userMapper.updateActiveUserStatus(userId, status, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException("User does not exist");
        }
        apiKeyAuthCacheService.evictByUserId(userId);
    }

    @Transactional
    /**
     * 删除用户及其关联数据。
     */
    public void deleteUser(Long userId) {
        AdminContext.requireAdmin();
        // 禁止删除当前登录管理员，避免把自己踢掉
        JwtUser currentUser = AdminContext.require();
        if (currentUser.userId().equals(userId)) {
            throw new BusinessException("Cannot delete the current admin user");
        }

        if (!userMapper.existsActiveById(userId)) {
            throw new BusinessException("User does not exist");
        }

        // 先清理关联数据，再删除用户主记录
        userCleanupMapper.deleteAgentToolLogsByUserId(userId);
        userCleanupMapper.deleteAgentMessagesByUserId(userId);
        userCleanupMapper.deleteAgentSessionsByUserId(userId);
        userCleanupMapper.deleteRequestLogsByUserId(userId);
        userCleanupMapper.deleteUsageDailyByUserId(userId);
        userCleanupMapper.deletePackageUsageDailyByUserId(userId);
        transactionMapper.deleteByUserId(userId);
        userCleanupMapper.deleteUserModelPackagesByUserId(userId);
        walletMapper.deleteByUserId(userId);
        apiKeyAuthCacheService.evictByUserId(userId);
        apiKeyMapper.deleteByUserId(userId);
        userMapper.deleteById(userId);
    }

    @Transactional
    /**
     * 给用户钱包充值。
     */
    public void recharge(WalletRechargeRequest request) {
        AdminContext.requireAdmin();
        // 查询目标用户钱包并计算充值后的余额
        WalletEntity wallet = walletMapper.selectByUserId(request.userId());
        if (wallet == null) {
            throw new BusinessException("Wallet does not exist");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.amount());

        // 更新钱包余额与累计充值金额
        WalletEntity updateWallet = new WalletEntity();
        updateWallet.setBalance(balanceAfter);
        updateWallet.setTotalRecharge(wallet.getTotalRecharge().add(request.amount()));
        updateWallet.setUpdatedAt(LocalDateTime.now());
        walletMapper.updateByUserId(request.userId(), updateWallet);
        apiKeyAuthCacheService.evictByUserId(request.userId());

        // 追加一条充值流水，方便后续审计
        TransactionEntity transaction = new TransactionEntity();
        transaction.setUserId(request.userId());
        transaction.setWalletId(wallet.getId());
        transaction.setOrderNo(buildOrderNo("R"));
        transaction.setTransactionType("RECHARGE");
        transaction.setDirection("IN");
        transaction.setAmount(request.amount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setStatus("SUCCESS");
        transaction.setDescriptionText(request.remark() == null || request.remark().isBlank() ? "Admin recharge" : request.remark());
        transaction.setTransactionDate(LocalDate.now());
        transactionMapper.insert(transaction);
    }

    /**
     * 把查询结果转换成接口返回对象。
     */
    private UserListItemResponse toUserListItemResponse(UserListView user) {
        return new UserListItemResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRoleCode(),
                user.getStatus(),
                user.getEmail(),
                user.getPhone(),
                user.getBalance(),
                zeroIfNull(user.getMaxConcurrentRequests()),
                zeroIfNull(user.getMaxConcurrentStreams()),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    /**
     * 生成交易订单号。
     */
    private String buildOrderNo(String prefix) {
        // 使用时间戳 + 随机串生成业务订单号
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 空白字符串转成 null。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 当值为空白时回退到默认值。
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 字段未传时保留原值。
     */
    private String fallbackBlank(String candidate, String fallback) {
        return candidate == null ? fallback : candidate;
    }

    private Integer normalizeConcurrencyLimit(Integer value) {
        return value == null || value <= 0 ? 0 : value;
    }

    private Integer zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
