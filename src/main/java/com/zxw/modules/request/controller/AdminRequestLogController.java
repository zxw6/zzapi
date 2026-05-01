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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/request-logs")
@Api(tags = "请求日志")
/**
 * 请求日志控制器。
 * 提供后台分页查看请求日志的接口。
 */
public class AdminRequestLogController {

    private final AdminRequestLogService adminRequestLogService;

    public AdminRequestLogController(AdminRequestLogService adminRequestLogService) {
        this.adminRequestLogService = adminRequestLogService;
    }

    /**
     * 分页查询最新请求日志列表。
     */
    @GetMapping
    @ApiOperation("分页查询请求日志")
    public ApiResponse<RequestLogPageResponse<RequestLogItemResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        // 按页码和每页条数返回请求日志，前端不再直接传 limit
        return ApiResponse.ok(adminRequestLogService.page(page, pageSize));
    }
}
