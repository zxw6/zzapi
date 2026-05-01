package com.zxw.modules.apikey.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.access.service.UserModelAccessService;
import com.zxw.modules.apikey.dto.ApiKeyCreateRequest;
import com.zxw.modules.apikey.dto.ApiKeyCreateResponse;
import com.zxw.modules.apikey.dto.ApiKeyListItemResponse;
import com.zxw.persistence.entity.ApiKeyEntity;
import com.zxw.persistence.mapper.ApiKeyMapper;
import com.zxw.persistence.mapper.ApiKeyQueryMapper;
import com.zxw.persistence.mapper.UserMapper;
import com.zxw.persistence.model.ApiKeyListView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
/**
 * API Key 管理服务。
 * 负责 API Key 的查询、创建、状态维护与删除。
 */
public class AdminApiKeyService {

    public static final int ACCESS_KEY_PREFIX_LENGTH = 20;

    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyQueryMapper apiKeyQueryMapper;
    private final UserMapper userMapper;
    private final PasswordService passwordService;
    private final UserModelAccessService userModelAccessService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminApiKeyService(ApiKeyMapper apiKeyMapper,
                              ApiKeyQueryMapper apiKeyQueryMapper,
                              UserMapper userMapper,
                              PasswordService passwordService,
                              UserModelAccessService userModelAccessService) {
        this.apiKeyMapper = apiKeyMapper;
        this.apiKeyQueryMapper = apiKeyQueryMapper;
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.userModelAccessService = userModelAccessService;
    }

    /**
     * 查询当前可见的 API Key 列表。
     */
    public List<ApiKeyListItemResponse> listApiKeys() {
        // 确保套餐默认数据已经就绪
        userModelAccessService.initializeDefaults();

        // 管理员查全部，普通用户只查自己的 API Key
        JwtUser currentUser = AdminContext.require();
        List<ApiKeyListView> rows = AdminContext.isAdmin()
                ? apiKeyQueryMapper.listAdmin()
                : apiKeyQueryMapper.listUser(currentUser.userId());

        return rows.stream()
                .map(item -> new ApiKeyListItemResponse(
                        item.getId(),
                        item.getUserId(),
                        item.getUsername(),
                        item.getName(),
                        item.getAccessKey(),
                        item.getStatus(),
                        item.getModelPackageId(),
                        item.getModelPackageName(),
                        item.getModelGroupId(),
                        item.getModelGroupName(),
                        defaultBigDecimal(item.getTotalQuota()),
                        defaultBigDecimal(item.getUsedQuota()),
                        item.getExpiresAt(),
                        item.getLastUsedAt(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    /**
     * 创建新的 API Key。
     */
    public ApiKeyCreateResponse create(ApiKeyCreateRequest request) {
        // 创建前先保证默认套餐配置已完成初始化
        userModelAccessService.initializeDefaults();

        // 无论当前账号是否为管理员，创建出的 API Key 都只能绑定当前登录账号自己的套餐。
        JwtUser currentUser = AdminContext.require();
        Long targetUserId = currentUser.userId();

        if (!userMapper.existsActiveById(targetUserId)) {
            throw new BusinessException("User not found");
        }

        // 解析本次 API Key 应该绑定的套餐和模型分组
        UserModelAccessService.ApiKeyPackageBinding packageBinding = userModelAccessService.resolveApiKeyPackageBinding(
                targetUserId,
                request.modelPackageId(),
                request.modelGroupId()
        );

        // 生成明文密钥，只保存前缀和哈希值
        String plainTextKey = generatePlainTextKey();
        String accessKey = plainTextKey.substring(0, ACCESS_KEY_PREFIX_LENGTH);
        LocalDateTime expiresAt = parseDateTime(request.expiresAt());

        // 插入 API Key 主记录
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setUserId(targetUserId);
        entity.setName(request.name());
        entity.setAccessKey(accessKey);
        entity.setSecretHash(passwordService.encode(plainTextKey));
        entity.setStatus("ACTIVE");
        entity.setExpiresAt(expiresAt);
        entity.setUserPackageId(packageBinding.packageId());
        entity.setModelGroupId(packageBinding.modelGroupId());
        entity.setTotalQuota(BigDecimal.ZERO);
        entity.setUsedQuota(BigDecimal.ZERO);
        entity.setRemark(request.remark());
        apiKeyMapper.insert(entity);

        return new ApiKeyCreateResponse(entity.getId(), plainTextKey);
    }

    /**
     * 更新 API Key 状态。
     */
    public void updateStatus(Long id, String status) {
        // 普通用户只能操作自己的 API Key
        JwtUser currentUser = AdminContext.require();
        int updated = apiKeyMapper.updateStatus(
                id,
                AdminContext.isAdmin() ? null : currentUser.userId(),
                status,
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw new BusinessException("API key not found");
        }
    }

    /**
     * 逻辑删除 API Key。
     */
    public void delete(Long id) {
        // 逻辑删除 API Key，保留历史数据
        JwtUser currentUser = AdminContext.require();
        int updated = apiKeyMapper.softDelete(
                id,
                AdminContext.isAdmin() ? null : currentUser.userId(),
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw new BusinessException("API key not found");
        }
    }

    /**
     * 对展示用的 Key 做脱敏处理。
     */
    public String maskKey(String accessKey) {
        // 对外展示时仅保留前后部分字符
        if (accessKey == null || accessKey.length() <= 8) {
            return accessKey;
        }
        return accessKey.substring(0, 6) + "******" + accessKey.substring(accessKey.length() - 4);
    }

    /**
     * 生成明文 API Key。
     */
    private String generatePlainTextKey() {
        // 使用随机字节生成不可预测的密钥
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return "sk-live-" + HexFormat.of().formatHex(bytes);
    }

    /**
     * 解析过期时间字符串。
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 空金额兜底成 0。
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
