package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel("管理员登录响应")
/**
 * 登录成功响应对象。
 */
/**
 * 登录成功响应对象。
 * 返回当前登录用户的身份信息和账户余额。
 */
public record AdminLoginResponse(
        @ApiModelProperty("登录令牌")
        String token,
        @ApiModelProperty("用户ID")
        Long userId,
        @ApiModelProperty("用户名")
        String username,
        @ApiModelProperty("昵称")
        String nickname,
        @ApiModelProperty("角色编码")
        String roleCode,
        @ApiModelProperty("账户余额")
        BigDecimal balance
) {
}
