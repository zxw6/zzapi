package com.zxw.modules.provider.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.provider.dto.ProviderCreateRequest;
import com.zxw.modules.provider.dto.ProviderListItemResponse;
import com.zxw.modules.provider.dto.ProviderStatusUpdateRequest;
import com.zxw.modules.provider.service.AdminProviderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/providers")
public class AdminProviderController {

    private final AdminProviderService adminProviderService;

    public AdminProviderController(AdminProviderService adminProviderService) {
        this.adminProviderService = adminProviderService;
    }

    @GetMapping
    public ApiResponse<List<ProviderListItemResponse>> list() {
        return ApiResponse.ok(adminProviderService.listProviders());
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody ProviderCreateRequest request) {
        adminProviderService.create(request);
        return ApiResponse.ok("渠道创建成功", null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ProviderStatusUpdateRequest request) {
        adminProviderService.updateStatus(id, request.status());
        return ApiResponse.ok("状态更新成功", null);
    }
}
