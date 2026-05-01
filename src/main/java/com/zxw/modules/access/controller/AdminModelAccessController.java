package com.zxw.modules.access.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.access.dto.ModelAccessSummaryResponse;
import com.zxw.modules.access.dto.ModelGroupCreateRequest;
import com.zxw.modules.access.dto.ModelGroupUpdateRequest;
import com.zxw.modules.access.dto.ModelPackagePurchaseRecordResponse;
import com.zxw.modules.access.dto.PurchaseModelPackageRequest;
import com.zxw.modules.access.service.UserModelAccessService;
import com.zxw.modules.user.dto.WalletTransactionItemResponse;
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
@RequestMapping("/admin/model-access")
@Api(tags = "模型套餐管理")
/**
 * 模型套餐控制器。
 * 提供套餐概览、购买记录、钱包流水和分组维护接口。
 */
public class AdminModelAccessController {

    private final UserModelAccessService userModelAccessService;

    public AdminModelAccessController(UserModelAccessService userModelAccessService) {
        this.userModelAccessService = userModelAccessService;
    }

    /**
     * 查询当前套餐概览。
     */
    @GetMapping("/summary")
    @ApiOperation("查询当前套餐概览")
    public ApiResponse<ModelAccessSummaryResponse> summary() {
        // 返回当前用户可用套餐与额度概览
        return ApiResponse.ok(userModelAccessService.getCurrentSummary());
    }

    /**
     * 创建套餐分组。
     */
    @PostMapping("/groups")
    @ApiOperation("创建套餐分组")
    public ApiResponse<ModelAccessSummaryResponse> createGroup(@Valid @RequestBody ModelGroupCreateRequest request) {
        // 新建模型套餐分组
        return ApiResponse.ok("套餐创建成功", userModelAccessService.createGroup(request));
    }

    /**
     * 修改套餐分组。
     */
    @PutMapping("/groups/{groupId}")
    @ApiOperation("修改套餐分组")
    public ApiResponse<ModelAccessSummaryResponse> updateGroup(@PathVariable Long groupId,
                                                               @Valid @RequestBody ModelGroupUpdateRequest request) {
        // 按套餐分组 id 修改名称、价格、额度和备注等配置
        return ApiResponse.ok("套餐修改成功", userModelAccessService.updateGroup(groupId, request));
    }

    /**
     * 购买模型套餐。
     */
    @PostMapping("/purchase")
    @ApiOperation("购买套餐")
    public ApiResponse<ModelAccessSummaryResponse> purchase(@Valid @RequestBody PurchaseModelPackageRequest request) {
        // 购买指定模型套餐
        return ApiResponse.ok("套餐购买成功", userModelAccessService.purchase(request));
    }

    /**
     * 查询套餐购买记录。
     */
    @GetMapping("/purchases")
    @ApiOperation("查询套餐购买记录")
    public ApiResponse<List<ModelPackagePurchaseRecordResponse>> purchases() {
        // 查看套餐购买历史
        return ApiResponse.ok(userModelAccessService.listPurchaseRecords());
    }

    /**
     * 查询钱包流水。
     */
    @GetMapping("/wallet-transactions")
    @ApiOperation("查询钱包流水")
    public ApiResponse<List<WalletTransactionItemResponse>> walletTransactions() {
        // 查看钱包充值和消费流水
        return ApiResponse.ok(userModelAccessService.listWalletTransactions());
    }

    /**
     * 逻辑禁用指定套餐分组。
     */
    @DeleteMapping("/{groupId}")
    @ApiOperation("禁用套餐分组")
    public ApiResponse<Void> delete(@PathVariable Long groupId) {
        // 逻辑禁用指定套餐分组
        userModelAccessService.disableGroup(groupId);
        return ApiResponse.ok("套餐分组已删除", null);
    }

    /**
     * 删除已购买套餐并禁用关联密钥。
     */
    @DeleteMapping("/purchases/{packageId}")
    @ApiOperation("删除已购买套餐")
    public ApiResponse<Void> deletePurchasedPackage(@PathVariable Long packageId) {
        // 删除已购买套餐并禁用关联的 API Key
        userModelAccessService.disablePurchasedPackage(packageId);
        return ApiResponse.ok("已购套餐删除成功", null);
    }
}
