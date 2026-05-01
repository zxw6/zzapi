package com.zxw.modules.model.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.model.dto.ModelBatchImportRequest;
import com.zxw.modules.model.dto.ModelBatchImportResponse;
import com.zxw.modules.model.dto.ModelCreateRequest;
import com.zxw.modules.model.dto.ModelListItemResponse;
import com.zxw.modules.model.dto.ModelStatusUpdateRequest;
import com.zxw.modules.model.dto.ModelUpdateRequest;
import com.zxw.modules.model.dto.UpstreamModelOptionResponse;
import com.zxw.modules.model.service.AdminModelService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/models")
@Api(tags = "模型管理")
/**
 * 模型管理控制器。
 * 提供模型列表、创建、更新、导入和状态维护接口。
 */
public class AdminModelController {

    private final AdminModelService adminModelService;

    public AdminModelController(AdminModelService adminModelService) {
        this.adminModelService = adminModelService;
    }

    /**
     * 查询模型列表。
     */
    @GetMapping
    @ApiOperation("查询模型列表")
    public ApiResponse<List<ModelListItemResponse>> list() {
        // 返回平台内已配置的模型清单
        return ApiResponse.ok(adminModelService.listModels());
    }

    /**
     * 从指定渠道拉取上游模型选项。
     */
    @GetMapping("/upstream")
    @ApiOperation("查询上游模型列表")
    public ApiResponse<List<UpstreamModelOptionResponse>> listUpstreamModels(@RequestParam Long providerId) {
        // 从指定渠道拉取可用上游模型
        return ApiResponse.ok(adminModelService.fetchUpstreamModels(providerId));
    }

    /**
     * 创建模型并绑定默认路由。
     */
    @PostMapping
    @ApiOperation("创建模型")
    public ApiResponse<Void> create(@Valid @RequestBody ModelCreateRequest request) {
        // 新建模型并绑定默认路由
        adminModelService.create(request);
        return ApiResponse.ok("模型创建成功", null);
    }

    /**
     * 更新模型与分组绑定信息。
     */
    @PutMapping("/{id}")
    @ApiOperation("更新模型")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ModelUpdateRequest request) {
        // 更新模型配置、路由和分组绑定
        adminModelService.update(id, request);
        return ApiResponse.ok("模型价格已更新", null);
    }

    /**
     * 删除模型及其路由绑定。
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除模型")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        // 删除模型及其绑定路由
        adminModelService.delete(id);
        return ApiResponse.ok("模型删除成功", null);
    }

    /**
     * 批量导入上游模型。
     */
    @PostMapping("/import")
    @ApiOperation("批量导入模型")
    public ApiResponse<ModelBatchImportResponse> batchImport(@Valid @RequestBody ModelBatchImportRequest request) {
        // 批量导入上游模型并建立绑定
        return ApiResponse.ok("模型批量导入成功", adminModelService.batchImport(request));
    }

    /**
     * 单独切换模型状态。
     */
    @PutMapping("/{id}/status")
    @ApiOperation("更新模型状态")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ModelStatusUpdateRequest request) {
        // 启用或禁用指定模型
        adminModelService.updateStatus(id, request.status());
        return ApiResponse.ok("状态更新成功", null);
    }
}
