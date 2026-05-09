package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ApiModel("发送验证码请求")
public record VerificationCodeSendRequest(
        @ApiModelProperty(value = "QQ邮箱", required = true)
        @NotBlank(message = "QQ邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Pattern(regexp = "^[^\\s@]+@qq\\.com$", message = "请使用QQ邮箱")
        String email
) {
}
