package com.zxw.modules.user.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.user.dto.UserCreateRequest;
import com.zxw.modules.user.dto.UserListItemResponse;
import com.zxw.modules.user.dto.UserStatusUpdateRequest;
import com.zxw.modules.user.dto.UserUpdateRequest;
import com.zxw.modules.user.dto.WalletRechargeRequest;
import com.zxw.modules.user.service.AdminUserService;
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
@RequestMapping("/admin/users")
@Api(tags = "用户管理")
/**
 * 用户管理控制器。
 * 提供用户列表、详情、状态维护以及钱包充值等后台接口。
 */
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @ApiOperation("查询用户列表")
    /**
     * 查询用户列表。
     */
    public ApiResponse<List<UserListItemResponse>> list() {
        // 管理员查看全部用户，普通用户查看自己
        return ApiResponse.ok(adminUserService.listUsers());
    }

    @GetMapping("/{userId}")
    @ApiOperation("查询用户详情")
    /**
     * 查询指定用户详情。
     */
    public ApiResponse<UserListItemResponse> detail(@PathVariable Long userId) {
        // 根据用户ID获取详情
        return ApiResponse.ok(adminUserService.getUser(userId));
    }

    @PostMapping
    @ApiOperation("创建用户")
    /**
     * 创建新用户。
     */
    public ApiResponse<Void> create(@Valid @RequestBody UserCreateRequest request) {
        // 管理员新建平台用户
        adminUserService.createUser(request);
        return ApiResponse.ok("User created", null);
    }

    @PutMapping("/{userId}")
    @ApiOperation("更新用户信息")
    /**
     * 更新用户基础信息。
     */
    public ApiResponse<Void> update(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        // 更新用户的基础信息和状态
        adminUserService.updateUser(userId, request);
        return ApiResponse.ok("User updated", null);
    }

    @PutMapping("/{userId}/status")
    @ApiOperation("更新用户状态")
    /**
     * 单独更新用户状态。
     */
    public ApiResponse<Void> updateStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusUpdateRequest request) {
        // 单独维护用户启用禁用状态
        adminUserService.updateStatus(userId, request.status());
        return ApiResponse.ok("User status updated", null);
    }

    @DeleteMapping("/{userId}")
    @ApiOperation("删除用户")
    /**
     * 删除指定用户。
     */
    public ApiResponse<Void> delete(@PathVariable Long userId) {
        // 删除指定用户及关联数据
        adminUserService.deleteUser(userId);
        return ApiResponse.ok("User deleted", null);
    }

    @PostMapping("/recharge")
    @ApiOperation("用户钱包充值")
    public ApiResponse<Void> recharge(@Valid @RequestBody WalletRechargeRequest request) {
        // 给指定用户钱包增加余额
        adminUserService.recharge(request);
        return ApiResponse.ok("Recharge completed", null);
    }
}
