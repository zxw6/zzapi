package com.zxw.modules.provider.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.provider.dto.ProviderCreateRequest;
import com.zxw.modules.provider.dto.ProviderListItemResponse;
import com.zxw.modules.provider.dto.ProviderStatusUpdateRequest;
import com.zxw.modules.provider.service.AdminProviderService;
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
@RequestMapping("/admin/providers")
@Api(tags = "渠道管理")
public class AdminProviderController {

    private final AdminProviderService adminProviderService;

    public AdminProviderController(AdminProviderService adminProviderService) {
        this.adminProviderService = adminProviderService;
    }

    @GetMapping
    @ApiOperation("查询渠道列表")
    public ApiResponse<List<ProviderListItemResponse>> list() {
        return ApiResponse.ok(adminProviderService.listProviders());
    }

    @PostMapping
    @ApiOperation("创建渠道")
    public ApiResponse<Void> create(@Valid @RequestBody ProviderCreateRequest request) {
        adminProviderService.create(request);
        return ApiResponse.ok("渠道创建成功", null);
    }

    @PutMapping("/{id}/status")
    @ApiOperation("修改渠道状态")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody ProviderStatusUpdateRequest request) {
        adminProviderService.updateStatus(id, request.status());
        return ApiResponse.ok("渠道状态修改成功", null);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除渠道")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminProviderService.deleteProvider(id);
        return ApiResponse.ok("渠道删除成功", null);
    }
}
