package com.zxw.modules.dashboard.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.dashboard.dto.DashboardModelStatResponse;
import com.zxw.modules.dashboard.dto.DashboardOverviewResponse;
import com.zxw.modules.dashboard.dto.DashboardTrendPointResponse;
import com.zxw.modules.dashboard.service.AdminDashboardService;
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
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> overview() {
        return ApiResponse.ok(dashboardService.getOverview());
    }

    @GetMapping("/trend")
    public ApiResponse<List<DashboardTrendPointResponse>> trend(
            @RequestParam(defaultValue = "7") @Min(1) @Max(30) int days) {
        return ApiResponse.ok(dashboardService.getRequestTrend(days));
    }

    @GetMapping("/model-stats")
    public ApiResponse<List<DashboardModelStatResponse>> modelStats() {
        return ApiResponse.ok(dashboardService.getModelStats());
    }
}
