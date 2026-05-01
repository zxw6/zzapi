package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ApiModel("用户注册请求")
/**
 * 用户注册请求对象。
 */
/**
 * 用户注册请求对象。
 * 用于接收注册页面提交的基础资料和验证码。
 */
public record UserRegisterRequest(
        @ApiModelProperty(value = "用户名", required = true)
        @NotBlank(message = "Username cannot be blank")
        String username,
        @ApiModelProperty(value = "密码", required = true)
        @NotBlank(message = "Password cannot be blank")
        String password,
        @ApiModelProperty("昵称")
        String nickname,
        @ApiModelProperty(value = "QQ邮箱", required = true)
        @NotBlank(message = "QQ email cannot be blank")
        @Email(message = "Email format is invalid")
        @Pattern(regexp = "^[^\\s@]+@qq\\.com$", message = "Please use a QQ email")
        String email,
        @ApiModelProperty("手机号")
        String phone,
        @ApiModelProperty(value = "验证码", required = true)
        @NotBlank(message = "Verification code cannot be blank")
        @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
        String verificationCode
) {
}
