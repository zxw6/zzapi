package com.zxw.modules.apikey.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.apikey.dto.ApiKeyCreateRequest;
import com.zxw.modules.apikey.dto.ApiKeyCreateResponse;
import com.zxw.modules.apikey.dto.ApiKeyListItemResponse;
import com.zxw.modules.apikey.dto.ApiKeyStatusUpdateRequest;
import com.zxw.modules.apikey.service.AdminApiKeyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/api-keys")
@Api(tags = "API Key管理")
/**
 * API Key 管理控制器。
 * 提供 API Key 列表、创建、状态更新和删除接口。
 */
public class AdminApiKeyController {

    private final AdminApiKeyService adminApiKeyService;

    public AdminApiKeyController(AdminApiKeyService adminApiKeyService) {
        this.adminApiKeyService = adminApiKeyService;
    }

    /**
     * 查询 API Key 列表。
     */
    @GetMapping
    @ApiOperation("查询API Key列表")
    public ApiResponse<List<ApiKeyListItemResponse>> list() {
        // 查询当前用户可见的 API Key 列表
        return ApiResponse.ok(adminApiKeyService.listApiKeys());
    }

    /**
     * 创建新的 API Key。
     */
    @PostMapping
    @ApiOperation("创建API Key")
    public ApiResponse<ApiKeyCreateResponse> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        // 创建新的 API Key 并绑定套餐
        return ApiResponse.ok("API Key 创建成功", adminApiKeyService.create(request));
    }

    /**
     * 更新 API Key 启用状态。
     */
    @PutMapping("/{id}/status")
    @ApiOperation("更新API Key状态")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ApiKeyStatusUpdateRequest request) {
        // 启用或禁用指定 API Key
        adminApiKeyService.updateStatus(id, request.status());
        return ApiResponse.ok("状态更新成功", null);
    }
    /**
     * 逻辑删除指定 API Key。
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除API Key")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        // 逻辑删除指定 API Key
        adminApiKeyService.delete(id);
        return ApiResponse.ok("API Key å·²åˆ é™¤", null);
    }
}
