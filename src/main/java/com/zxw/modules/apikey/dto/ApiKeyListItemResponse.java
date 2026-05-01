package com.zxw.modules.apikey.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApiModel("API Key列表项响应")
/**
 * API Key 列表项响应对象。
 */
/**
 * API Key 列表项响应对象。
 * 用于后台展示密钥状态、额度和套餐绑定信息。
 */
public record ApiKeyListItemResponse(
        @ApiModelProperty("主键ID")
        Long id,
        @ApiModelProperty("用户ID")
        Long userId,
        @ApiModelProperty("用户名")
        String username,
        @ApiModelProperty("名称")
        String name,
        @ApiModelProperty("访问Key前缀")
        String accessKey,
        @ApiModelProperty("状态")
        String status,
        @ApiModelProperty("套餐ID")
        Long modelPackageId,
        @ApiModelProperty("套餐名称")
        String modelPackageName,
        @ApiModelProperty("模型分组ID")
        Long modelGroupId,
        @ApiModelProperty("模型分组名称")
        String modelGroupName,
        @ApiModelProperty("总额度")
        BigDecimal totalQuota,
        @ApiModelProperty("已使用额度")
        BigDecimal usedQuota,
        @ApiModelProperty("过期时间")
        LocalDateTime expiresAt,
        @ApiModelProperty("最后使用时间")
        LocalDateTime lastUsedAt,
        @ApiModelProperty("创建时间")
        LocalDateTime createdAt
) {
}
