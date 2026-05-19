package com.zxw.modules.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApiModel("用户列表项响应")
/**
 * 用户列表项响应对象。
 */
/**
 * 用户列表项响应对象。
 * 用于后台展示用户资料、角色和钱包余额。
 */
public record UserListItemResponse(
        @ApiModelProperty("用户ID")
        Long id,
        @ApiModelProperty("用户名")
        String username,
        @ApiModelProperty("昵称")
        String nickname,
        @ApiModelProperty("角色编码")
        String roleCode,
        @ApiModelProperty("状态")
        String status,
        @ApiModelProperty("邮箱")
        String email,
        @ApiModelProperty("手机号")
        String phone,
        @ApiModelProperty("余额")
        BigDecimal balance,
        @ApiModelProperty("最大并发请求数，0表示使用全局默认")
        Integer maxConcurrentRequests,
        @ApiModelProperty("最大并发流式请求数，0表示使用全局默认")
        Integer maxConcurrentStreams,
        @ApiModelProperty("最后登录时间")
        LocalDateTime lastLoginAt,
        @ApiModelProperty("创建时间")
        LocalDateTime createdAt
) {
}
