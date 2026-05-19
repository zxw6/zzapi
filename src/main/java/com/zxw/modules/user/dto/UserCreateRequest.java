package com.zxw.modules.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
        BigDecimal initialBalance,
        @ApiModelProperty("最大并发请求数，0表示使用全局默认")
        @Min(value = 0, message = "最大并发请求数不能小于 0")
        Integer maxConcurrentRequests,
        @ApiModelProperty("最大并发流式请求数，0表示使用全局默认")
        @Min(value = 0, message = "最大并发流式请求数不能小于 0")
        Integer maxConcurrentStreams
) {
}
