package com.zxw.modules.auth.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.auth.dto.AdminLoginRequest;
import com.zxw.modules.auth.dto.AdminLoginResponse;
import com.zxw.modules.auth.dto.UserRegisterRequest;
import com.zxw.modules.auth.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok("登录成功", adminAuthService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<AdminLoginResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.ok("注册成功", adminAuthService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<AdminLoginResponse> me() {
        return ApiResponse.ok(adminAuthService.me());
    }
}
