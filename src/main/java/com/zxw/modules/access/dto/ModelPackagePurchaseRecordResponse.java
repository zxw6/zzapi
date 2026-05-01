package com.zxw.modules.access.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApiModel("套餐购买记录响应")
/**
 * 套餐购买记录响应对象。
 */
/**
 * 套餐购买记录响应对象。
 * 用于后台查看每次购买的额度、周期和状态。
 */
public record ModelPackagePurchaseRecordResponse(
        @ApiModelProperty("购买记录 id")
        Long id,
        @ApiModelProperty("用户 id")
        Long userId,
        @ApiModelProperty("用户名")
        String username,
        @ApiModelProperty("套餐分组 id")
        Long groupId,
        @ApiModelProperty("套餐分组编码")
        String groupCode,
        @ApiModelProperty("套餐分组名称")
        String groupName,
        @ApiModelProperty("模型数量")
        Integer modelCount,
        @ApiModelProperty("购买价格")
        BigDecimal purchasePrice,
        @ApiModelProperty("开始时间")
        LocalDateTime startAt,
        @ApiModelProperty("过期时间")
        LocalDateTime expiresAt,
        @ApiModelProperty("状态")
        String status,
        @ApiModelProperty("创建时间")
        LocalDateTime createdAt,
        @ApiModelProperty("当前是否有效")
        boolean active,
        @ApiModelProperty("日额度")
        BigDecimal dailyQuota,
        @ApiModelProperty("周额度")
        BigDecimal weeklyQuota,
        @ApiModelProperty("月额度")
        BigDecimal monthlyQuota,
        @ApiModelProperty("总额度")
        BigDecimal totalQuota,
        @ApiModelProperty("今日已用额度")
        BigDecimal dailyUsed,
        @ApiModelProperty("本周已用额度")
        BigDecimal weeklyUsed,
        @ApiModelProperty("本月已用额度")
        BigDecimal monthlyUsed,
        @ApiModelProperty("累计已用额度")
        BigDecimal totalUsed,
        @ApiModelProperty("剩余天数")
        Long remainingDays
) {
}
