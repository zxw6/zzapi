package com.zxw.modules.dashboard.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.dashboard.dto.DashboardModelStatResponse;
import com.zxw.modules.dashboard.dto.DashboardOverviewResponse;
import com.zxw.modules.dashboard.dto.DashboardTrendPointResponse;
import com.zxw.modules.dashboard.service.AdminDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/admin/dashboard")
@Api(tags = "仪表盘")
/**
 * 仪表盘控制器。
 * 提供总览、趋势和模型统计等可视化数据接口。
 */
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询仪表盘总览信息。
     */
    @GetMapping("/overview")
    @ApiOperation("查询仪表盘概览")
    public ApiResponse<DashboardOverviewResponse> overview() {
        // 返回平台总览统计
        return ApiResponse.ok(dashboardService.getOverview());
    }

    /**
     * 按天统计请求趋势。
     */
    @GetMapping("/trend")
    @ApiOperation("查询请求趋势")
    public ApiResponse<List<DashboardTrendPointResponse>> trend(
            @RequestParam(defaultValue = "7") @Min(1) @Max(30) int days) {
        // 按天统计近一段时间请求趋势
        return ApiResponse.ok(dashboardService.getRequestTrend(days));
    }

    /**
     * 汇总各模型的调用统计。
     */
    @GetMapping("/model-stats")
    @ApiOperation("查询模型统计")
    public ApiResponse<List<DashboardModelStatResponse>> modelStats() {
        // 汇总各模型的调用统计
        return ApiResponse.ok(dashboardService.getModelStats());
    }
}
