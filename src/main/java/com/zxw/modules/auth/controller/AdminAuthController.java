package com.zxw.modules.auth.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.auth.dto.AdminLoginRequest;
import com.zxw.modules.auth.dto.AdminLoginResponse;
import com.zxw.modules.auth.dto.LoginCaptchaResponse;
import com.zxw.modules.auth.dto.PasswordResetRequest;
import com.zxw.modules.auth.dto.UserRegisterRequest;
import com.zxw.modules.auth.dto.VerificationCodeSendRequest;
import com.zxw.modules.auth.dto.VerificationCodeSendResponse;
import com.zxw.modules.auth.service.AdminAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@Api(tags = "认证管理")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/login/captcha")
    @ApiOperation("获取登录图形验证码")
    public ApiResponse<LoginCaptchaResponse> loginCaptcha() {
        return ApiResponse.ok("获取图形验证码成功", adminAuthService.createLoginCaptcha());
    }

    @PostMapping("/login")
    @ApiOperation("登录")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok("登录成功", adminAuthService.login(request));
    }

    @PostMapping("/register/code")
    @ApiOperation("发送注册验证码")
    public ApiResponse<VerificationCodeSendResponse> sendRegisterCode(
            @Valid @RequestBody VerificationCodeSendRequest request) {
        return ApiResponse.ok("验证码发送成功", adminAuthService.sendRegisterCode(request));
    }

    @PostMapping("/register")
    @ApiOperation("注册")
    public ApiResponse<AdminLoginResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.ok("注册成功", adminAuthService.register(request));
    }

    @PostMapping("/password/reset/code")
    @ApiOperation("发送重置密码验证码")
    public ApiResponse<VerificationCodeSendResponse> sendPasswordResetCode(
            @Valid @RequestBody VerificationCodeSendRequest request) {
        return ApiResponse.ok("重置密码验证码发送成功", adminAuthService.sendPasswordResetCode(request));
    }

    @PostMapping("/password/reset")
    @ApiOperation("根据QQ邮箱重置密码")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        adminAuthService.resetPassword(request);
        return ApiResponse.ok("密码重置成功", null);
    }

    @GetMapping("/me")
    @ApiOperation("当前登录用户")
    public ApiResponse<AdminLoginResponse> me() {
        return ApiResponse.ok(adminAuthService.me());
    }
}
