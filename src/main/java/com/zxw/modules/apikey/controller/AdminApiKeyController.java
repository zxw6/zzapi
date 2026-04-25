package com.zxw.modules.apikey.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.apikey.dto.ApiKeyCreateRequest;
import com.zxw.modules.apikey.dto.ApiKeyCreateResponse;
import com.zxw.modules.apikey.dto.ApiKeyListItemResponse;
import com.zxw.modules.apikey.dto.ApiKeyStatusUpdateRequest;
import com.zxw.modules.apikey.service.AdminApiKeyService;
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
public class AdminApiKeyController {

    private final AdminApiKeyService adminApiKeyService;

    public AdminApiKeyController(AdminApiKeyService adminApiKeyService) {
        this.adminApiKeyService = adminApiKeyService;
    }

    @GetMapping
    public ApiResponse<List<ApiKeyListItemResponse>> list() {
        return ApiResponse.ok(adminApiKeyService.listApiKeys());
    }

    @PostMapping
    public ApiResponse<ApiKeyCreateResponse> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        return ApiResponse.ok("API Key 创建成功", adminApiKeyService.create(request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ApiKeyStatusUpdateRequest request) {
        adminApiKeyService.updateStatus(id, request.status());
        return ApiResponse.ok("状态更新成功", null);
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminApiKeyService.delete(id);
        return ApiResponse.ok("API Key å·²åˆ é™¤", null);
    }
}
