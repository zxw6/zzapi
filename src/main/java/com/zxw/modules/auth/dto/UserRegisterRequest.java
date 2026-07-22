package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ApiModel("用户注册请求")
public record UserRegisterRequest(
        @ApiModelProperty(value = "用户名", required = true)
        @NotBlank(message = "用户名不能为空")
        String username,
        @ApiModelProperty(value = "密码", required = true)
        @NotBlank(message = "密码不能为空")
        String password,
        @ApiModelProperty("昵称")
        String nickname,
        @ApiModelProperty(value = "QQ邮箱", required = true)
        @NotBlank(message = "QQ邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Pattern(regexp = "^[^\\s@]+@qq\\.com$", message = "请使用QQ邮箱")
        String email,
        @ApiModelProperty("手机号")
        String phone,
        @ApiModelProperty(value = "验证码", required = true)
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
        String verificationCode
) {
}
