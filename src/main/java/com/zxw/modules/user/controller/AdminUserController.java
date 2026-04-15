package com.zxw.modules.user.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.user.dto.UserCreateRequest;
import com.zxw.modules.user.dto.UserListItemResponse;
import com.zxw.modules.user.dto.UserStatusUpdateRequest;
import com.zxw.modules.user.dto.UserUpdateRequest;
import com.zxw.modules.user.dto.WalletRechargeRequest;
import com.zxw.modules.user.service.AdminUserService;
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
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<UserListItemResponse>> list() {
        return ApiResponse.ok(adminUserService.listUsers());
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserListItemResponse> detail(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.getUser(userId));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody UserCreateRequest request) {
        adminUserService.createUser(request);
        return ApiResponse.ok("User created", null);
    }

    @PutMapping("/{userId}")
    public ApiResponse<Void> update(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        adminUserService.updateUser(userId, request);
        return ApiResponse.ok("User updated", null);
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusUpdateRequest request) {
        adminUserService.updateStatus(userId, request.status());
        return ApiResponse.ok("User status updated", null);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ApiResponse.ok("User deleted", null);
    }

    @PostMapping("/recharge")
    public ApiResponse<Void> recharge(@Valid @RequestBody WalletRechargeRequest request) {
        adminUserService.recharge(request);
        return ApiResponse.ok("Recharge completed", null);
    }
}
