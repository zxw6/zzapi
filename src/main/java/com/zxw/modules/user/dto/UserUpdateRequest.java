package com.zxw.modules.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("更新用户请求")
/**
 * 用户信息更新请求对象。
 */
/**
 * 用户信息更新请求对象。
 * 用于修改昵称、联系方式、角色和密码等信息。
 */
public record UserUpdateRequest(
        @ApiModelProperty("昵称")
        String nickname,
        @ApiModelProperty("邮箱")
        String email,
        @ApiModelProperty("手机号")
        String phone,
        @ApiModelProperty("角色编码")
        String roleCode,
        @ApiModelProperty("状态")
        String status,
        @ApiModelProperty("新密码")
        String password
) {
}
