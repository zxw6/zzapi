package com.zxw.modules.access.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.access.dto.ModelAccessSummaryResponse;
import com.zxw.modules.access.dto.ModelGroupCreateRequest;
import com.zxw.modules.access.dto.ModelPackagePurchaseRecordResponse;
import com.zxw.modules.access.dto.PurchaseModelPackageRequest;
import com.zxw.modules.access.service.UserModelAccessService;
import com.zxw.modules.user.dto.WalletTransactionItemResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/model-access")
public class AdminModelAccessController {

    private final UserModelAccessService userModelAccessService;

    public AdminModelAccessController(UserModelAccessService userModelAccessService) {
        this.userModelAccessService = userModelAccessService;
    }

    @GetMapping("/summary")
    public ApiResponse<ModelAccessSummaryResponse> summary() {
        return ApiResponse.ok(userModelAccessService.getCurrentSummary());
    }

    @PostMapping("/groups")
    public ApiResponse<ModelAccessSummaryResponse> createGroup(@Valid @RequestBody ModelGroupCreateRequest request) {
        return ApiResponse.ok("套餐创建成功", userModelAccessService.createGroup(request));
    }

    @PostMapping("/purchase")
    public ApiResponse<ModelAccessSummaryResponse> purchase(@Valid @RequestBody PurchaseModelPackageRequest request) {
        return ApiResponse.ok("套餐购买成功", userModelAccessService.purchase(request));
    }

    @GetMapping("/purchases")
    public ApiResponse<List<ModelPackagePurchaseRecordResponse>> purchases() {
        return ApiResponse.ok(userModelAccessService.listPurchaseRecords());
    }

    @GetMapping("/wallet-transactions")
    public ApiResponse<List<WalletTransactionItemResponse>> walletTransactions() {
        return ApiResponse.ok(userModelAccessService.listWalletTransactions());
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> delete(@PathVariable Long groupId) {
        userModelAccessService.disableGroup(groupId);
        return new ApiResponse<>(true, "套餐分组已删除", null);
    }
}
