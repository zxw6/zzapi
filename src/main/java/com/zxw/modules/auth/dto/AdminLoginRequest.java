package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;

@ApiModel("管理员登录请求")
/**
 * 管理员登录请求对象。
 * 用于接收后台登录时提交的用户名、密码和图形验证码。
 */
public record AdminLoginRequest(
        @ApiModelProperty(value = "用户名", required = true)
        @NotBlank(message = "用户名不能为空")
        String username,
        @ApiModelProperty(value = "密码", required = true)
        @NotBlank(message = "密码不能为空")
        String password,
        @ApiModelProperty(value = "验证码ID", required = true)
        @NotBlank(message = "验证码标识不能为空")
        String captchaId,
        @ApiModelProperty(value = "图形验证码", required = true)
        @NotBlank(message = "图形验证码不能为空")
        String captchaCode
) {
}
