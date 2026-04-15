package com.zxw.modules.apikey.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.PasswordService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApiKeyAuthService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;

    public ApiKeyAuthService(JdbcTemplate jdbcTemplate, PasswordService passwordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
    }

    public AuthenticatedApiKey authenticate(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank() || bearerToken.length() < AdminApiKeyService.ACCESS_KEY_PREFIX_LENGTH) {
            throw new BusinessException(401, "API Key 无效");
        }
        String accessKey = bearerToken.substring(0, AdminApiKeyService.ACCESS_KEY_PREFIX_LENGTH);
        List<AuthenticatedApiKey> items = jdbcTemplate.query("""
                select k.id, k.user_id, k.secret_hash, k.status, k.total_quota, k.used_quota, k.expires_at,
                       u.username, u.role_code, coalesce(w.balance, 0) as balance
                from api_keys k
                join users u on u.id = k.user_id and u.deleted = 0
                left join wallets w on w.user_id = u.id
                where k.access_key = ? and k.deleted = 0
                """, (rs, rowNum) -> new AuthenticatedApiKey(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("role_code"),
                rs.getString("secret_hash"),
                rs.getString("status"),
                rs.getBigDecimal("total_quota"),
                rs.getBigDecimal("used_quota"),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getBigDecimal("balance")
        ), accessKey);

        for (AuthenticatedApiKey item : items) {
            if (passwordService.matches(bearerToken, item.secretHash())) {
                if (!"ACTIVE".equals(item.status())) {
                    throw new BusinessException(403, "API Key 已被禁用");
                }
                if (item.expiresAt() != null && item.expiresAt().isBefore(LocalDateTime.now())) {
                    throw new BusinessException(403, "API Key 已过期");
                }
                return item;
            }
        }
        throw new BusinessException(401, "API Key 无效");
    }

    public void markUsed(Long apiKeyId) {
        jdbcTemplate.update("update api_keys set last_used_at = now(), updated_at = now() where id = ?", apiKeyId);
    }

    public record AuthenticatedApiKey(
            Long id,
            Long userId,
            String username,
            String roleCode,
            String secretHash,
            String status,
            BigDecimal totalQuota,
            BigDecimal usedQuota,
            LocalDateTime expiresAt,
            BigDecimal balance
    ) {
    }
}