package com.zxw.modules.apikey.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ApiModel("创建API Key请求")
/**
 * API Key 创建请求对象。
 */
/**
 * API Key 创建请求对象。
 * 用于接收新密钥的归属用户、套餐绑定和过期设置。
 */
public record ApiKeyCreateRequest(
        @ApiModelProperty(value = "用户ID", required = true)
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @ApiModelProperty(value = "密钥名称", required = true)
        @NotBlank(message = "密钥名称不能为空")
        String name,
        @ApiModelProperty("套餐ID")
        Long modelPackageId,
        @ApiModelProperty("模型分组ID")
        Long modelGroupId,
        @ApiModelProperty("过期时间，格式 yyyy-MM-dd HH:mm:ss")
        String expiresAt,
        @ApiModelProperty("备注")
        String remark
) {
}
