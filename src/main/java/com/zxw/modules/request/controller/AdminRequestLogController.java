package com.zxw.modules.request.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.request.dto.RequestLogItemResponse;
import com.zxw.modules.request.dto.RequestLogPageResponse;
import com.zxw.modules.request.service.AdminRequestLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/request-logs")
@Api(tags = "Request logs")
public class AdminRequestLogController {

    private final AdminRequestLogService adminRequestLogService;

    public AdminRequestLogController(AdminRequestLogService adminRequestLogService) {
        this.adminRequestLogService = adminRequestLogService;
    }

    @GetMapping
    @ApiOperation("List request logs")
    public ApiResponse<RequestLogPageResponse<RequestLogItemResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(adminRequestLogService.page(page, pageSize));
    }

    @DeleteMapping
    @ApiOperation("Clear request logs")
    public ApiResponse<Integer> clear(@RequestParam(required = false) Long userId) {
        return ApiResponse.ok("请求日志已清除，套餐额度统计不受影响", adminRequestLogService.clear(userId));
    }
}
