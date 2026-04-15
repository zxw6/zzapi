package com.zxw.modules.request.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.request.dto.RequestLogItemResponse;
import com.zxw.modules.request.service.AdminRequestLogService;
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
@RequestMapping("/admin/request-logs")
public class AdminRequestLogController {

    private final AdminRequestLogService adminRequestLogService;

    public AdminRequestLogController(AdminRequestLogService adminRequestLogService) {
        this.adminRequestLogService = adminRequestLogService;
    }

    @GetMapping
    public ApiResponse<List<RequestLogItemResponse>> list(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ApiResponse.ok(adminRequestLogService.latest(limit));
    }
}
