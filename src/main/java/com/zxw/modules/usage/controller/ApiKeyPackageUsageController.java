package com.zxw.modules.usage.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.common.exception.BusinessException;
import com.zxw.modules.usage.dto.ApiKeyPackageUsageResponse;
import com.zxw.modules.usage.service.ApiKeyPackageUsageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/key-usage", "/key-usage"})
@Api(tags = "API Key 用量查询")
public class ApiKeyPackageUsageController {

    private final ApiKeyPackageUsageService packageUsageService;

    public ApiKeyPackageUsageController(ApiKeyPackageUsageService packageUsageService) {
        this.packageUsageService = packageUsageService;
    }

    @GetMapping("/package")
    @ApiOperation("根据 API Key 查询绑定套餐使用情况")
    public ApiResponse<ApiKeyPackageUsageResponse> packageUsage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-API-Key", required = false) String xApiKey) {
        return ApiResponse.ok(packageUsageService.getPackageUsage(extractApiKey(authorization, xApiKey)));
    }

    private String extractApiKey(String authorization, String xApiKey) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String apiKey = authorization.substring(7).trim();
            if (!apiKey.isBlank()) {
                return apiKey;
            }
        }
        if (xApiKey != null && !xApiKey.isBlank()) {
            return xApiKey.trim();
        }
        throw new BusinessException(401, "Missing API key");
    }
}
