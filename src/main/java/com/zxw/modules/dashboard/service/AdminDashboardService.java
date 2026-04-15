package com.zxw.modules.dashboard.service;

import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.dashboard.dto.DashboardModelStatResponse;
import com.zxw.modules.dashboard.dto.DashboardOverviewResponse;
import com.zxw.modules.dashboard.dto.DashboardTrendPointResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Collections;
import java.util.List;

@Service
public class AdminDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardService(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
    }

    public DashboardOverviewResponse getOverview() {
        if (jdbcTemplate == null) {
            return emptyOverview();
        }

        JwtUser currentUser = AdminContext.require();
        if (!AdminContext.isAdmin()) {
            return jdbcTemplate.queryForObject("""
                    select
                        1 as user_count,
                        (select count(*) from api_keys where deleted = 0 and user_id = ?) as api_key_count,
                        0 as provider_count,
                        (select count(*) from models where deleted = 0 and status = 'ACTIVE' and is_public = 1) as model_count,
                        (select count(*) from request_logs where request_date = curdate() and user_id = ?) as request_count_today,
                        coalesce((select sum(total_tokens) from request_logs where request_date = curdate() and user_id = ?), 0) as total_tokens_today,
                        coalesce((select sum(total_tokens) from usage_daily where user_id = ? and stat_date >= curdate() - interval 6 day), 0) as total_tokens_7d,
                        coalesce((select sum(case when direction = 'IN' then amount else 0 end)
                                  from transactions
                                  where transaction_date = curdate() and user_id = ?), 0) as recharge_amount_today,
                        coalesce((select sum(case when direction = 'OUT' then amount else 0 end)
                                  from transactions
                                  where transaction_date = curdate() and user_id = ?), 0) as consume_amount_today,
                        coalesce((select balance from wallets where user_id = ?), 0) as wallet_balance_total
                    """, (rs, rowNum) -> new DashboardOverviewResponse(
                    rs.getLong("user_count"),
                    rs.getLong("api_key_count"),
                    rs.getLong("provider_count"),
                    rs.getLong("model_count"),
                    rs.getLong("request_count_today"),
                    rs.getLong("total_tokens_today"),
                    rs.getLong("total_tokens_7d"),
                    rs.getBigDecimal("recharge_amount_today"),
                    rs.getBigDecimal("consume_amount_today"),
                    rs.getBigDecimal("wallet_balance_total")
            ), currentUser.userId(), currentUser.userId(), currentUser.userId(), currentUser.userId(), currentUser.userId(), currentUser.userId(), currentUser.userId());
        }

        return jdbcTemplate.queryForObject("""
                select
                    (select count(*) from users) as user_count,
                    (select count(*) from api_keys where deleted = 0) as api_key_count,
                    (select count(*) from providers where deleted = 0) as provider_count,
                    (select count(*) from models where deleted = 0) as model_count,
                    (select count(*) from request_logs where request_date = curdate()) as request_count_today,
                    coalesce((select sum(total_tokens) from request_logs where request_date = curdate()), 0) as total_tokens_today,
                    coalesce((select sum(total_tokens) from usage_daily where stat_date >= curdate() - interval 6 day), 0) as total_tokens_7d,
                    coalesce((select sum(case when direction = 'IN' then amount else 0 end)
                              from transactions
                              where transaction_date = curdate()), 0) as recharge_amount_today,
                    coalesce((select sum(case when direction = 'OUT' then amount else 0 end)
                              from transactions
                              where transaction_date = curdate()), 0) as consume_amount_today,
                    coalesce((select sum(balance) from wallets), 0) as wallet_balance_total
                """, (rs, rowNum) -> new DashboardOverviewResponse(
                rs.getLong("user_count"),
                rs.getLong("api_key_count"),
                rs.getLong("provider_count"),
                rs.getLong("model_count"),
                rs.getLong("request_count_today"),
                rs.getLong("total_tokens_today"),
                rs.getLong("total_tokens_7d"),
                rs.getBigDecimal("recharge_amount_today"),
                rs.getBigDecimal("consume_amount_today"),
                rs.getBigDecimal("wallet_balance_total")
        ));
    }

    public List<DashboardTrendPointResponse> getRequestTrend(int days) {
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        JwtUser currentUser = AdminContext.require();
        if (!AdminContext.isAdmin()) {
            return jdbcTemplate.query("""
                    select stat_date, request_count, success_count, total_tokens, user_amount, cost_amount
                    from usage_daily
                    where user_id = ? and stat_date >= curdate() - interval ? day
                    order by stat_date asc
                    """, (rs, rowNum) -> new DashboardTrendPointResponse(
                    rs.getObject("stat_date", Date.class).toLocalDate(),
                    rs.getLong("request_count"),
                    rs.getLong("success_count"),
                    rs.getLong("total_tokens"),
                    rs.getBigDecimal("user_amount"),
                    rs.getBigDecimal("cost_amount")
            ), currentUser.userId(), Math.max(days - 1, 0));
        }

        return jdbcTemplate.query("""
                select stat_date, request_count, success_count, total_tokens, user_amount, cost_amount
                from usage_daily
                where stat_date >= curdate() - interval ? day
                order by stat_date asc
                """, (rs, rowNum) -> new DashboardTrendPointResponse(
                rs.getObject("stat_date", Date.class).toLocalDate(),
                rs.getLong("request_count"),
                rs.getLong("success_count"),
                rs.getLong("total_tokens"),
                rs.getBigDecimal("user_amount"),
                rs.getBigDecimal("cost_amount")
        ), Math.max(days - 1, 0));
    }

    public List<DashboardModelStatResponse> getModelStats() {
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        JwtUser currentUser = AdminContext.require();
        if (!AdminContext.isAdmin()) {
            return jdbcTemplate.query("""
                    select model_code,
                           coalesce(group_concat(distinct upstream_model order by upstream_model separator ', '), '') as upstream_models,
                           count(*) as request_count,
                           coalesce(sum(total_tokens), 0) as total_tokens,
                           coalesce(avg(latency_ms), 0) as avg_latency_ms,
                           coalesce(sum(latency_ms), 0) as total_latency_ms,
                           coalesce(sum(case when success = 1 then 1 else 0 end) * 100.0 / count(*), 0) as success_rate,
                           coalesce(sum(user_amount), 0) as user_amount
                    from request_logs
                    where user_id = ?
                      and request_date >= date_format(curdate(), '%Y-%m-01')
                    group by model_code
                    order by request_count desc, total_tokens desc, model_code asc
                    """, (rs, rowNum) -> new DashboardModelStatResponse(
                    rs.getString("model_code"),
                    rs.getString("upstream_models"),
                    rs.getLong("request_count"),
                    rs.getLong("total_tokens"),
                    rs.getDouble("avg_latency_ms"),
                    rs.getLong("total_latency_ms"),
                    rs.getDouble("success_rate"),
                    rs.getBigDecimal("user_amount")
            ), currentUser.userId());
        }

        return jdbcTemplate.query("""
                select model_code,
                       coalesce(group_concat(distinct upstream_model order by upstream_model separator ', '), '') as upstream_models,
                       count(*) as request_count,
                       coalesce(sum(total_tokens), 0) as total_tokens,
                       coalesce(avg(latency_ms), 0) as avg_latency_ms,
                       coalesce(sum(latency_ms), 0) as total_latency_ms,
                       coalesce(sum(case when success = 1 then 1 else 0 end) * 100.0 / count(*), 0) as success_rate,
                       coalesce(sum(user_amount), 0) as user_amount
                from request_logs
                where request_date >= date_format(curdate(), '%Y-%m-01')
                group by model_code
                order by request_count desc, total_tokens desc, model_code asc
                """, (rs, rowNum) -> new DashboardModelStatResponse(
                rs.getString("model_code"),
                rs.getString("upstream_models"),
                rs.getLong("request_count"),
                rs.getLong("total_tokens"),
                rs.getDouble("avg_latency_ms"),
                rs.getLong("total_latency_ms"),
                rs.getDouble("success_rate"),
                rs.getBigDecimal("user_amount")
        ));
    }

    private DashboardOverviewResponse emptyOverview() {
        return new DashboardOverviewResponse(
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
