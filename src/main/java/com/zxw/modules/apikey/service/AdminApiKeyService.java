package com.zxw.modules.apikey.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.access.service.UserModelAccessService;
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
    private final UserModelAccessService userModelAccessService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminApiKeyService(JdbcTemplate jdbcTemplate,
                              PasswordService passwordService,
                              UserModelAccessService userModelAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
        this.userModelAccessService = userModelAccessService;
    }

    public List<ApiKeyListItemResponse> listApiKeys() {
        userModelAccessService.initializeDefaults();

        JwtUser currentUser = AdminContext.require();
        boolean groupSchemaReady = hasColumn("api_keys", "model_group_id") && hasTable("model_groups");
        String groupSelect = groupSchemaReady
                ? "g.id as model_group_id, g.group_name as model_group_name,"
                : "null as model_group_id, null as model_group_name,";
        String groupJoin = groupSchemaReady
                ? "left join model_groups g on g.id = k.model_group_id"
                : "";
        if (!AdminContext.isAdmin()) {
            return jdbcTemplate.query("""
                    select k.id, k.user_id, u.username, k.name, k.access_key, k.status,
                           %s
                           k.total_quota, k.used_quota, k.expires_at, k.last_used_at, k.created_at
                    from api_keys k
                    join users u on u.id = k.user_id
                    %s
                    where k.deleted = 0 and k.user_id = ?
                    order by k.id desc
                    """.formatted(groupSelect, groupJoin), (rs, rowNum) -> new ApiKeyListItemResponse(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("access_key"),
                    rs.getString("status"),
                    rs.getObject("model_group_id") == null ? null : rs.getLong("model_group_id"),
                    rs.getString("model_group_name"),
                    rs.getBigDecimal("total_quota"),
                    rs.getBigDecimal("used_quota"),
                    toLocalDateTime(rs.getTimestamp("expires_at")),
                    toLocalDateTime(rs.getTimestamp("last_used_at")),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ), currentUser.userId());
        }

        return jdbcTemplate.query("""
                select k.id, k.user_id, u.username, k.name, k.access_key, k.status,
                       %s
                       k.total_quota, k.used_quota, k.expires_at, k.last_used_at, k.created_at
                from api_keys k
                join users u on u.id = k.user_id
                %s
                where k.deleted = 0
                order by k.id desc
                """.formatted(groupSelect, groupJoin), (rs, rowNum) -> new ApiKeyListItemResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("access_key"),
                rs.getString("status"),
                rs.getObject("model_group_id") == null ? null : rs.getLong("model_group_id"),
                rs.getString("model_group_name"),
                rs.getBigDecimal("total_quota"),
                rs.getBigDecimal("used_quota"),
                toLocalDateTime(rs.getTimestamp("expires_at")),
                toLocalDateTime(rs.getTimestamp("last_used_at")),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    @Transactional
    public ApiKeyCreateResponse create(ApiKeyCreateRequest request) {
        userModelAccessService.initializeDefaults();

        JwtUser currentUser = AdminContext.require();
        Long targetUserId = AdminContext.isAdmin() ? request.userId() : currentUser.userId();

        Integer userExists = jdbcTemplate.queryForObject(
                "select count(*) from users where id = ? and deleted = 0",
                Integer.class,
                targetUserId
        );
        if (userExists == null || userExists == 0) {
            throw new BusinessException("用户不存在");
        }
        userModelAccessService.validateApiKeyCreationAccess(targetUserId, request.modelGroupId());
        Long resolvedModelGroupId = userModelAccessService.resolveApiKeyModelGroupId(targetUserId, request.modelGroupId());

        String plainTextKey = generatePlainTextKey();
        String accessKey = plainTextKey.substring(0, ACCESS_KEY_PREFIX_LENGTH);
        LocalDateTime expiresAt = parseDateTime(request.expiresAt());

        jdbcTemplate.update("""
                insert into api_keys (user_id, name, access_key, secret_hash, status, expires_at, model_group_id, total_quota, used_quota, remark)
                values (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, 0, ?)
                """,
                targetUserId,
                request.name(),
                accessKey,
                passwordService.encode(plainTextKey),
                expiresAt,
                resolvedModelGroupId,
                BigDecimal.ZERO,
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
            throw new BusinessException("API Key 不存在");
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

    private boolean hasTable(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
