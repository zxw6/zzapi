package com.zxw.modules.dashboard.service;

import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.dashboard.dto.DashboardModelStatResponse;
import com.zxw.modules.dashboard.dto.DashboardOverviewResponse;
import com.zxw.modules.dashboard.dto.DashboardTrendPointResponse;
import com.zxw.persistence.mapper.DashboardQueryMapper;
import com.zxw.persistence.model.DashboardModelStatView;
import com.zxw.persistence.model.DashboardOverviewView;
import com.zxw.persistence.model.DashboardTrendView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
/**
 * 后台仪表盘服务。
 * 负责按当前身份组装总览、趋势和模型维度统计数据。
 */
public class AdminDashboardService {

    private final DashboardQueryMapper dashboardQueryMapper;

    public AdminDashboardService(DashboardQueryMapper dashboardQueryMapper) {
        this.dashboardQueryMapper = dashboardQueryMapper;
    }

    /**
     * 查询仪表盘总览。
     */
    public DashboardOverviewResponse getOverview() {
        JwtUser currentUser = AdminContext.require();
        DashboardOverviewView overview = AdminContext.isAdmin()
                ? dashboardQueryMapper.selectAdminOverview()
                : dashboardQueryMapper.selectUserOverview(currentUser.userId());
        return overview == null ? emptyOverview() : toOverviewResponse(overview);
    }

    /**
     * 查询最近若干天的请求趋势。
     */
    public List<DashboardTrendPointResponse> getRequestTrend(int days) {
        JwtUser currentUser = AdminContext.require();
        int daysBack = Math.max(days - 1, 0);
        List<DashboardTrendView> rows = AdminContext.isAdmin()
                ? dashboardQueryMapper.selectAdminTrend(daysBack)
                : dashboardQueryMapper.selectUserTrend(currentUser.userId(), daysBack);
        return rows.stream()
                .map(item -> new DashboardTrendPointResponse(
                        item.getStatDate(),
                        defaultLong(item.getRequestCount()),
                        defaultLong(item.getSuccessCount()),
                        defaultLong(item.getTotalTokens()),
                        defaultBigDecimal(item.getUserAmount()),
                        defaultBigDecimal(item.getCostAmount())
                ))
                .toList();
    }

    /**
     * 查询模型维度统计结果。
     */
    public List<DashboardModelStatResponse> getModelStats() {
        JwtUser currentUser = AdminContext.require();
        List<DashboardModelStatView> rows = AdminContext.isAdmin()
                ? dashboardQueryMapper.selectAdminModelStats()
                : dashboardQueryMapper.selectUserModelStats(currentUser.userId());
        return rows.stream()
                .map(item -> new DashboardModelStatResponse(
                        item.getModelCode(),
                        item.getUpstreamModels(),
                        defaultLong(item.getRequestCount()),
                        defaultLong(item.getTotalTokens()),
                        item.getAvgLatencyMs() == null ? 0D : item.getAvgLatencyMs(),
                        defaultLong(item.getTotalLatencyMs()),
                        item.getSuccessRate() == null ? 0D : item.getSuccessRate(),
                        defaultBigDecimal(item.getUserAmount())
                ))
                .toList();
    }

    /**
     * 把数据库视图对象转换为接口返回对象。
     */
    private DashboardOverviewResponse toOverviewResponse(DashboardOverviewView overview) {
        return new DashboardOverviewResponse(
                defaultLong(overview.getUserCount()),
                defaultLong(overview.getApiKeyCount()),
                defaultLong(overview.getProviderCount()),
                defaultLong(overview.getModelCount()),
                defaultLong(overview.getRequestCountToday()),
                defaultLong(overview.getTotalTokensToday()),
                defaultLong(overview.getTotalTokens7d()),
                defaultBigDecimal(overview.getRechargeAmountToday()),
                defaultBigDecimal(overview.getConsumeAmountToday()),
                defaultBigDecimal(overview.getWalletBalanceTotal())
        );
    }

    /**
     * 构造空的默认总览对象。
     */
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

    /**
     * 空 Long 值兜底成 0。
     */
    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 空金额兜底成 0。
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
