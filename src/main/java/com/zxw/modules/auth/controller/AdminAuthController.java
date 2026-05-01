package com.zxw.modules.auth.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.auth.dto.AdminLoginRequest;
import com.zxw.modules.auth.dto.AdminLoginResponse;
import com.zxw.modules.auth.dto.LoginCaptchaResponse;
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
@Api(tags = "管理员认证")
/**
 * 认证控制器。
 * 提供登录、注册、验证码发送和当前用户信息查询接口。
 */
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * 获取登录图形验证码。
     */
    @GetMapping("/login/captcha")
    @ApiOperation("获取登录图形验证码")
    public ApiResponse<LoginCaptchaResponse> loginCaptcha() {
        // 返回登录页需要展示的图形验证码
        return ApiResponse.ok("获取图形验证码成功", adminAuthService.createLoginCaptcha());
    }

    /**
     * 管理员登录并返回令牌。
     */
    @PostMapping("/login")
    @ApiOperation("管理员登录")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        // 使用用户名密码完成登录
        return ApiResponse.ok("登录成功", adminAuthService.login(request));
    }

    /**
     * 发送注册验证码到邮箱。
     */
    @PostMapping("/register/code")
    @ApiOperation("发送注册验证码")
    public ApiResponse<VerificationCodeSendResponse> sendRegisterCode(
            @Valid @RequestBody VerificationCodeSendRequest request) {
        // 向注册邮箱发送验证码
        return ApiResponse.ok("验证码发送成功", adminAuthService.sendRegisterCode(request));
    }

    /**
     * 完成用户注册并自动登录。
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public ApiResponse<AdminLoginResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        // 校验验证码后创建新用户
        return ApiResponse.ok("注册成功", adminAuthService.register(request));
    }

    /**
     * 获取当前登录用户资料。
     */
    @GetMapping("/me")
    @ApiOperation("获取当前登录用户信息")
    public ApiResponse<AdminLoginResponse> me() {
        // 返回当前登录用户的基础资料
        return ApiResponse.ok(adminAuthService.me());
    }
}
