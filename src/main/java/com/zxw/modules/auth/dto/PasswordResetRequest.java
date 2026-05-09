package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ApiModel("重置密码请求")
public record PasswordResetRequest(
        @ApiModelProperty(value = "QQ邮箱", required = true)
        @NotBlank(message = "QQ邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Pattern(regexp = "^[^\\s@]+@qq\\.com$", message = "请使用QQ邮箱")
        String email,
        @ApiModelProperty(value = "验证码", required = true)
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
        String verificationCode,
        @ApiModelProperty(value = "新密码", required = true)
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 72, message = "新密码长度必须为6-72位")
        String newPassword
) {
}
