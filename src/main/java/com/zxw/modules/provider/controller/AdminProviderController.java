package com.zxw.modules.provider.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.provider.dto.ProviderCreateRequest;
import com.zxw.modules.provider.dto.ProviderListItemResponse;
import com.zxw.modules.provider.dto.ProviderStatusUpdateRequest;
import com.zxw.modules.provider.service.AdminProviderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "渠道管理")
/**
 * 渠道管理控制器。
 * 提供上游渠道的列表、创建和状态维护接口。
 */
public class AdminProviderController {

    private final AdminProviderService adminProviderService;

    public AdminProviderController(AdminProviderService adminProviderService) {
        this.adminProviderService = adminProviderService;
    }

    /**
     * 获取渠道列表。
     */
    @GetMapping
    @ApiOperation("查询渠道列表")
    public ApiResponse<List<ProviderListItemResponse>> list() {
        // 返回当前已配置的渠道列表
        return ApiResponse.ok(adminProviderService.listProviders());
    }

    /**
     * 新增一个上游渠道。
     */
    @PostMapping
    @ApiOperation("创建渠道")
    public ApiResponse<Void> create(@Valid @RequestBody ProviderCreateRequest request) {
        // 新建渠道并可选创建默认令牌
        adminProviderService.create(request);
        return ApiResponse.ok("渠道创建成功", null);
    }

    /**
     * 更新渠道启用状态。
     */
    @PutMapping("/{id}/status")
    @ApiOperation("更新渠道状态")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ProviderStatusUpdateRequest request) {
        // 更新渠道启用禁用状态
        adminProviderService.updateStatus(id, request.status());
        return ApiResponse.ok("状态更新成功", null);
    }
}
