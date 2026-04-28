package com.zxw.modules.request.service;

import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.request.dto.RequestLogItemResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class AdminRequestLogService {

    private final JdbcTemplate jdbcTemplate;

    public AdminRequestLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RequestLogItemResponse> latest(int limit) {
        JwtUser currentUser = AdminContext.require();
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        if (!tableExists("request_logs")) {
            return List.of();
        }

        boolean hasPackageLogColumn = columnExists("request_logs", "user_package_id");
        boolean hasCachedPromptTokens = columnExists("request_logs", "cached_prompt_tokens");
        boolean hasPackageTables = hasPackageLogColumn
                && tableExists("user_model_packages")
                && tableExists("model_groups");
        boolean hasModelMultiplier = tableExists("models") && columnExists("models", "multiplier");

        String packageNameSelect = hasPackageTables
                ? "coalesce(g.group_name, p.package_name) as package_name"
                : "cast(null as char) as package_name";
        String multiplierSelect = hasModelMultiplier
                ? "m.multiplier"
                : "cast(1.0000 as decimal(10,4)) as multiplier";
        String cachedPromptTokensSelect = hasCachedPromptTokens
                ? "l.cached_prompt_tokens"
                : "0 as cached_prompt_tokens";

        String joins = """
                from request_logs l
                left join users u on u.id = l.user_id
                """;
        if (hasPackageTables) {
            joins += """
                    left join user_model_packages p on p.id = l.user_package_id
                    left join model_groups g on g.id = p.group_id
                    """;
        }
        if (hasModelMultiplier) {
            joins += "left join models m on m.model_code = l.model_code and m.deleted = 0\n";
        }

        String sql = """
                select l.request_id, u.username, l.model_code, l.upstream_model, l.status_code,
                       %s, %s,
                       l.latency_ms, l.prompt_tokens, l.completion_tokens, l.total_tokens, %s,
                       l.user_amount, l.cost_amount, l.success, l.created_at
                %s
                %s
                order by l.id desc
                limit ?
                """.formatted(
                packageNameSelect,
                multiplierSelect,
                cachedPromptTokensSelect,
                joins,
                AdminContext.isAdmin() ? "" : "where l.user_id = ?"
        );

        if (!AdminContext.isAdmin()) {
            return jdbcTemplate.query(sql, requestLogMapper(), currentUser.userId(), safeLimit);
        }

        return jdbcTemplate.query(sql, requestLogMapper(), safeLimit);
    }

    private RowMapper<RequestLogItemResponse> requestLogMapper() {
        return (rs, rowNum) -> {
            Timestamp createdAt = rs.getTimestamp("created_at");
            return new RequestLogItemResponse(
                    rs.getString("request_id"),
                    rs.getString("username"),
                    rs.getString("model_code"),
                    rs.getString("upstream_model"),
                    rs.getString("package_name"),
                    rs.getBigDecimal("multiplier"),
                    rs.getInt("status_code"),
                    rs.getInt("latency_ms"),
                    rs.getInt("prompt_tokens"),
                    rs.getInt("completion_tokens"),
                    rs.getInt("total_tokens"),
                    rs.getInt("cached_prompt_tokens"),
                    rs.getBigDecimal("user_amount"),
                    rs.getBigDecimal("cost_amount"),
                    rs.getInt("success"),
                    createdAt == null ? null : createdAt.toLocalDateTime()
            );
        };
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
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
