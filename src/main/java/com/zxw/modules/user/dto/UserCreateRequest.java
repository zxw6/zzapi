package com.zxw.modules.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@ApiModel("创建用户请求")
/**
 * 用户创建请求对象。
 */
/**
 * 用户创建请求对象。
 * 用于后台新增用户并初始化基础账户信息。
 */
public record UserCreateRequest(
        @ApiModelProperty(value = "用户名", required = true)
        @NotBlank(message = "用户名不能为空")
        String username,
        @ApiModelProperty(value = "密码", required = true)
        @NotBlank(message = "密码不能为空")
        String password,
        @ApiModelProperty("昵称")
        String nickname,
        @ApiModelProperty("邮箱")
        String email,
        @ApiModelProperty("手机号")
        String phone,
        @ApiModelProperty("角色编码")
        String roleCode,
        @ApiModelProperty("初始余额")
        @DecimalMin(value = "0.00", message = "初始余额不能小于 0")
        BigDecimal initialBalance
) {
}
