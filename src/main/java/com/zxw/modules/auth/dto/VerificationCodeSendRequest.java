package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ApiModel("发送验证码请求")
/**
 * 验证码发送请求对象。
 * 用于接收需要发送验证码的邮箱地址。
 */
public record VerificationCodeSendRequest(
        @ApiModelProperty(value = "QQ邮箱", required = true)
        @NotBlank(message = "QQ 邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Pattern(regexp = "^[^\\s@]+@qq\\.com$", message = "请使用 QQ 邮箱")
        String email
) {
}
