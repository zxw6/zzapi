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
public class AdminModelController {

    private final AdminModelService adminModelService;

    public AdminModelController(AdminModelService adminModelService) {
        this.adminModelService = adminModelService;
    }

    @GetMapping
    public ApiResponse<List<ModelListItemResponse>> list() {
        return ApiResponse.ok(adminModelService.listModels());
    }

    @GetMapping("/upstream")
    public ApiResponse<List<UpstreamModelOptionResponse>> listUpstreamModels(@RequestParam Long providerId) {
        return ApiResponse.ok(adminModelService.fetchUpstreamModels(providerId));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody ModelCreateRequest request) {
        adminModelService.create(request);
        return ApiResponse.ok("模型创建成功", null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ModelUpdateRequest request) {
        adminModelService.update(id, request);
        return ApiResponse.ok("模型价格已更新", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminModelService.delete(id);
        return ApiResponse.ok("模型删除成功", null);
    }

    @PostMapping("/import")
    public ApiResponse<ModelBatchImportResponse> batchImport(@Valid @RequestBody ModelBatchImportRequest request) {
        return ApiResponse.ok("模型批量导入成功", adminModelService.batchImport(request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ModelStatusUpdateRequest request) {
        adminModelService.updateStatus(id, request.status());
        return ApiResponse.ok("状态更新成功", null);
    }
}
