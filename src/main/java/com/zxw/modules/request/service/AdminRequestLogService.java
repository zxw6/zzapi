package com.zxw.modules.request.service;

import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.request.dto.RequestLogItemResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
        if (!AdminContext.isAdmin()) {
            return jdbcTemplate.query("""
                    select l.request_id, u.username, l.model_code, l.upstream_model, l.status_code,
                           l.latency_ms, l.total_tokens, l.user_amount, l.cost_amount, l.success, l.created_at
                    from request_logs l
                    left join users u on u.id = l.user_id
                    where l.user_id = ?
                    order by l.id desc
                    limit ?
                    """, (rs, rowNum) -> new RequestLogItemResponse(
                    rs.getString("request_id"),
                    rs.getString("username"),
                    rs.getString("model_code"),
                    rs.getString("upstream_model"),
                    rs.getInt("status_code"),
                    rs.getInt("latency_ms"),
                    rs.getInt("total_tokens"),
                    rs.getBigDecimal("user_amount"),
                    rs.getBigDecimal("cost_amount"),
                    rs.getInt("success"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ), currentUser.userId(), safeLimit);
        }

        return jdbcTemplate.query("""
                select l.request_id, u.username, l.model_code, l.upstream_model, l.status_code,
                       l.latency_ms, l.total_tokens, l.user_amount, l.cost_amount, l.success, l.created_at
                from request_logs l
                left join users u on u.id = l.user_id
                order by l.id desc
                limit ?
                """, (rs, rowNum) -> new RequestLogItemResponse(
                rs.getString("request_id"),
                rs.getString("username"),
                rs.getString("model_code"),
                rs.getString("upstream_model"),
                rs.getInt("status_code"),
                rs.getInt("latency_ms"),
                rs.getInt("total_tokens"),
                rs.getBigDecimal("user_amount"),
                rs.getBigDecimal("cost_amount"),
                rs.getInt("success"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), safeLimit);
    }
}
