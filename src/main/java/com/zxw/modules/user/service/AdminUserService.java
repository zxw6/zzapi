package com.zxw.modules.user.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.user.dto.UserCreateRequest;
import com.zxw.modules.user.dto.UserListItemResponse;
import com.zxw.modules.user.dto.UserUpdateRequest;
import com.zxw.modules.user.dto.WalletRechargeRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;

    public AdminUserService(JdbcTemplate jdbcTemplate, PasswordService passwordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
    }

    public List<UserListItemResponse> listUsers() {
        JwtUser currentUser = AdminContext.require();
        if (!AdminContext.isAdmin()) {
            return List.of(getUser(currentUser.userId()));
        }

        return jdbcTemplate.query("""
                select u.id, u.username, u.nickname, u.role_code, u.status, u.email, u.phone,
                       u.last_login_at, u.created_at, coalesce(w.balance, 0) as balance
                from users u
                left join wallets w on w.user_id = u.id
                where u.deleted = 0
                order by u.id desc
                """, (rs, rowNum) -> new UserListItemResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("role_code"),
                rs.getString("status"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getBigDecimal("balance"),
                rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public UserListItemResponse getUser(Long userId) {
        JwtUser currentUser = AdminContext.require();
        Long targetUserId = AdminContext.isAdmin() ? userId : currentUser.userId();

        List<UserListItemResponse> users = jdbcTemplate.query("""
                select u.id, u.username, u.nickname, u.role_code, u.status, u.email, u.phone,
                       u.last_login_at, u.created_at, coalesce(w.balance, 0) as balance
                from users u
                left join wallets w on w.user_id = u.id
                where u.deleted = 0 and u.id = ?
                limit 1
                """, (rs, rowNum) -> new UserListItemResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("role_code"),
                rs.getString("status"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getBigDecimal("balance"),
                rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), targetUserId);

        if (users.isEmpty()) {
            throw new BusinessException("User does not exist");
        }
        return users.get(0);
    }

    @Transactional
    public void createUser(UserCreateRequest request) {
        AdminContext.requireAdmin();
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where username = ? and deleted = 0",
                Integer.class,
                request.username()
        );
        if (count != null && count > 0) {
            throw new BusinessException("Username already exists");
        }

        String roleCode = request.roleCode() == null || request.roleCode().isBlank() ? "USER" : request.roleCode();
        jdbcTemplate.update("""
                insert into users (username, password_hash, nickname, email, phone, role_code, status, package_restriction_enabled)
                values (?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                """,
                request.username(),
                passwordService.encode(request.password()),
                blankToNull(request.nickname()),
                blankToNull(request.email()),
                blankToNull(request.phone()),
                roleCode,
                "ADMIN".equalsIgnoreCase(roleCode) ? 0 : 1
        );

        Long userId = jdbcTemplate.queryForObject(
                "select id from users where username = ? and deleted = 0",
                Long.class,
                request.username()
        );
        BigDecimal initialBalance = request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance();
        jdbcTemplate.update("""
                insert into wallets (user_id, balance, frozen_balance, total_recharge, total_consume)
                values (?, ?, 0, ?, 0)
                """, userId, initialBalance, initialBalance);

        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            jdbcTemplate.update("""
                    insert into transactions (user_id, wallet_id, order_no, transaction_type, direction, amount,
                                              balance_before, balance_after, status, description_text, transaction_date)
                    select ?, w.id, ?, 'RECHARGE', 'IN', ?, 0, ?, 'SUCCESS', ?, curdate()
                    from wallets w where w.user_id = ?
                    """,
                    userId,
                    buildOrderNo("R"),
                    initialBalance,
                    initialBalance,
                    "Initial balance on user creation",
                    userId
            );
        }
    }

    @Transactional
    public void updateUser(Long userId, UserUpdateRequest request) {
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        Long targetUserId = admin ? userId : currentUser.userId();

        UserListItemResponse targetUser = getUser(targetUserId);
        String nextRoleCode = admin ? defaultIfBlank(request.roleCode(), targetUser.roleCode()) : targetUser.roleCode();
        String nextStatus = admin ? defaultIfBlank(request.status(), targetUser.status()) : targetUser.status();
        String nextNickname = fallbackBlank(request.nickname(), targetUser.nickname());
        String nextEmail = fallbackBlank(request.email(), targetUser.email());
        String nextPhone = fallbackBlank(request.phone(), targetUser.phone());

        if (nextEmail != null && !nextEmail.isBlank()) {
            Integer emailExists = jdbcTemplate.queryForObject("""
                    select count(*)
                    from users
                    where email = ? and id <> ? and deleted = 0
                    """, Integer.class, nextEmail, targetUserId);
            if (emailExists != null && emailExists > 0) {
                throw new BusinessException("Email is already used by another user");
            }
        }

        if (!admin && request.roleCode() != null && !request.roleCode().isBlank()) {
            throw new BusinessException("Normal users cannot change role");
        }
        if (!admin && request.status() != null && !request.status().isBlank()) {
            throw new BusinessException("Normal users cannot change status");
        }

        String passwordHash = null;
        if (request.password() != null && !request.password().isBlank()) {
            passwordHash = passwordService.encode(request.password());
        }

        jdbcTemplate.update("""
                update users
                set nickname = ?, email = ?, phone = ?, role_code = ?, status = ?,
                    password_hash = coalesce(?, password_hash),
                    updated_at = now()
                where id = ? and deleted = 0
                """,
                blankToNull(nextNickname),
                blankToNull(nextEmail),
                blankToNull(nextPhone),
                nextRoleCode,
                nextStatus,
                passwordHash,
                targetUserId
        );
    }

    public void updateStatus(Long userId, String status) {
        AdminContext.requireAdmin();
        int updated = jdbcTemplate.update("""
                update users
                set status = ?, updated_at = now()
                where id = ? and deleted = 0
                """, status, userId);
        if (updated == 0) {
            throw new BusinessException("User does not exist");
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        AdminContext.requireAdmin();
        JwtUser currentUser = AdminContext.require();
        if (currentUser.userId().equals(userId)) {
            throw new BusinessException("Cannot delete the current admin user");
        }

        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from users where id = ? and deleted = 0",
                Integer.class,
                userId
        );
        if (exists == null || exists == 0) {
            throw new BusinessException("User does not exist");
        }

        jdbcTemplate.update("""
                delete from agent_tool_logs
                where session_id in (select id from agent_sessions where user_id = ?)
                """, userId);
        jdbcTemplate.update("""
                delete from agent_messages
                where session_id in (select id from agent_sessions where user_id = ?)
                """, userId);
        jdbcTemplate.update("""
                delete from agent_sessions
                where user_id = ?
                """, userId);
        jdbcTemplate.update("""
                delete from request_logs
                where user_id = ?
                """, userId);
        jdbcTemplate.update("""
                delete from usage_daily
                where user_id = ?
                """, userId);
        jdbcTemplate.update("""
                delete from transactions
                where user_id = ?
                """, userId);
        jdbcTemplate.update("""
                delete from user_model_packages
                where user_id = ?
                """, userId);
        jdbcTemplate.update("""
                delete from wallets
                where user_id = ?
                """, userId);
        jdbcTemplate.update("""
                delete from api_keys
                where user_id = ?
                """, userId);
        jdbcTemplate.update("""
                delete from users
                where id = ?
                """, userId);
    }

    @Transactional
    public void recharge(WalletRechargeRequest request) {
        AdminContext.requireAdmin();
        List<BigDecimal> balances = jdbcTemplate.query("""
                select balance
                from wallets
                where user_id = ?
                """, (rs, rowNum) -> rs.getBigDecimal("balance"), request.userId());
        if (balances.isEmpty()) {
            throw new BusinessException("Wallet does not exist");
        }
        BigDecimal balanceBefore = balances.get(0);
        BigDecimal balanceAfter = balanceBefore.add(request.amount());

        jdbcTemplate.update("""
                update wallets
                set balance = ?, total_recharge = total_recharge + ?, updated_at = now()
                where user_id = ?
                """, balanceAfter, request.amount(), request.userId());

        Long walletId = jdbcTemplate.queryForObject("select id from wallets where user_id = ?", Long.class, request.userId());
        jdbcTemplate.update("""
                insert into transactions (user_id, wallet_id, order_no, transaction_type, direction, amount,
                                          balance_before, balance_after, status, description_text, transaction_date)
                values (?, ?, ?, 'RECHARGE', 'IN', ?, ?, ?, 'SUCCESS', ?, curdate())
                """,
                request.userId(),
                walletId,
                buildOrderNo("R"),
                request.amount(),
                balanceBefore,
                balanceAfter,
                request.remark() == null || request.remark().isBlank() ? "Admin recharge" : request.remark()
        );
    }

    private String buildOrderNo(String prefix) {
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String fallbackBlank(String candidate, String fallback) {
        return candidate == null ? fallback : candidate;
    }
}

