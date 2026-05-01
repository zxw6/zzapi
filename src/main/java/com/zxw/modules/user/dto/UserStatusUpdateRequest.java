package com.zxw.modules.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;

@ApiModel("更新用户状态请求")
/**
 * 用户状态更新请求对象。
 */
/**
 * 用户状态更新请求对象。
 * 用于启用或禁用指定用户。
 */
public record UserStatusUpdateRequest(
        @ApiModelProperty(value = "状态", required = true)
        @NotBlank(message = "状态不能为空")
        String status
) {
}
