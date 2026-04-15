package com.zxw.modules.apikey.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.apikey.dto.ApiKeyCreateRequest;
import com.zxw.modules.apikey.dto.ApiKeyCreateResponse;
import com.zxw.modules.apikey.dto.ApiKeyListItemResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
public class AdminApiKeyService {

    public static final int ACCESS_KEY_PREFIX_LENGTH = 20;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminApiKeyService(JdbcTemplate jdbcTemplate, PasswordService passwordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
    }

    public List<ApiKeyListItemResponse> listApiKeys() {
        JwtUser currentUser = AdminContext.require();
        if (!AdminContext.isAdmin()) {
            return jdbcTemplate.query("""
                    select k.id, k.user_id, u.username, k.name, k.access_key, k.status,
                           k.total_quota, k.used_quota, k.expires_at, k.last_used_at, k.created_at
                    from api_keys k
                    join users u on u.id = k.user_id
                    where k.deleted = 0 and k.user_id = ?
                    order by k.id desc
                    """, (rs, rowNum) -> new ApiKeyListItemResponse(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("access_key"),
                    rs.getString("status"),
                    rs.getBigDecimal("total_quota"),
                    rs.getBigDecimal("used_quota"),
                    toLocalDateTime(rs.getTimestamp("expires_at")),
                    toLocalDateTime(rs.getTimestamp("last_used_at")),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ), currentUser.userId());
        }

        return jdbcTemplate.query("""
                select k.id, k.user_id, u.username, k.name, k.access_key, k.status,
                       k.total_quota, k.used_quota, k.expires_at, k.last_used_at, k.created_at
                from api_keys k
                join users u on u.id = k.user_id
                where k.deleted = 0
                order by k.id desc
                """, (rs, rowNum) -> new ApiKeyListItemResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("access_key"),
                rs.getString("status"),
                rs.getBigDecimal("total_quota"),
                rs.getBigDecimal("used_quota"),
                toLocalDateTime(rs.getTimestamp("expires_at")),
                toLocalDateTime(rs.getTimestamp("last_used_at")),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    @Transactional
    public ApiKeyCreateResponse create(ApiKeyCreateRequest request) {
        JwtUser currentUser = AdminContext.require();
        Long targetUserId = AdminContext.isAdmin() ? request.userId() : currentUser.userId();

        Integer userExists = jdbcTemplate.queryForObject(
                "select count(*) from users where id = ? and deleted = 0",
                Integer.class,
                targetUserId
        );
        if (userExists == null || userExists == 0) {
            throw new BusinessException("User does not exist");
        }

        String plainTextKey = generatePlainTextKey();
        String accessKey = plainTextKey.substring(0, ACCESS_KEY_PREFIX_LENGTH);
        BigDecimal totalQuota = request.totalQuota() == null ? BigDecimal.ZERO : request.totalQuota();
        LocalDateTime expiresAt = parseDateTime(request.expiresAt());

        jdbcTemplate.update("""
                insert into api_keys (user_id, name, access_key, secret_hash, status, expires_at, total_quota, used_quota, remark)
                values (?, ?, ?, ?, 'ACTIVE', ?, ?, 0, ?)
                """,
                targetUserId,
                request.name(),
                accessKey,
                passwordService.encode(plainTextKey),
                expiresAt,
                totalQuota,
                request.remark()
        );

        Long id = jdbcTemplate.queryForObject("""
                select id from api_keys
                where user_id = ? and access_key = ? and deleted = 0
                order by id desc
                limit 1
                """, Long.class, targetUserId, accessKey);
        return new ApiKeyCreateResponse(id, plainTextKey);
    }

    public void updateStatus(Long id, String status) {
        JwtUser currentUser = AdminContext.require();
        int updated;
        if (AdminContext.isAdmin()) {
            updated = jdbcTemplate.update("""
                    update api_keys
                    set status = ?, updated_at = now()
                    where id = ? and deleted = 0
                    """, status, id);
        } else {
            updated = jdbcTemplate.update("""
                    update api_keys
                    set status = ?, updated_at = now()
                    where id = ? and user_id = ? and deleted = 0
                    """, status, id, currentUser.userId());
        }
        if (updated == 0) {
            throw new BusinessException("API key does not exist");
        }
    }

    public String maskKey(String accessKey) {
        if (accessKey == null || accessKey.length() <= 8) {
            return accessKey;
        }
        return accessKey.substring(0, 6) + "******" + accessKey.substring(accessKey.length() - 4);
    }

    private String generatePlainTextKey() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return "sk-live-" + HexFormat.of().formatHex(bytes);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
