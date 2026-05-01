package com.zxw.modules.access.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApiModel("套餐分组选项响应")
/**
 * 模型套餐分组选项响应对象。
 */
/**
 * 套餐分组选项响应对象。
 * 用于前端下拉或卡片中展示套餐购买选项。
 */
public record ModelGroupOptionResponse(
        @ApiModelProperty("分组 id")
        Long id,
        @ApiModelProperty("分组编码")
        String groupCode,
        @ApiModelProperty("分组名称")
        String groupName,
        @ApiModelProperty("售价")
        BigDecimal salePrice,
        @ApiModelProperty("有效天数")
        Integer packageDays,
        @ApiModelProperty("日额度")
        BigDecimal dailyQuota,
        @ApiModelProperty("周额度")
        BigDecimal weeklyQuota,
        @ApiModelProperty("月额度")
        BigDecimal monthlyQuota,
        @ApiModelProperty("模型数量")
        Integer modelCount,
        @ApiModelProperty("是否已购买")
        boolean purchased,
        @ApiModelProperty("是否激活中")
        boolean active,
        @ApiModelProperty("过期时间")
        LocalDateTime expiresAt,
        @ApiModelProperty("剩余天数")
        Long remainingDays,
        @ApiModelProperty("今日已用额度")
        BigDecimal dailyUsed,
        @ApiModelProperty("本周已用额度")
        BigDecimal weeklyUsed,
        @ApiModelProperty("本月已用额度")
        BigDecimal monthlyUsed,
        @ApiModelProperty("套餐状态")
        String packageStatus,
        @ApiModelProperty("套餐状态说明")
        String packageStatusText,
        @ApiModelProperty("备注")
        String remark,
        @ApiModelProperty("是否系统预置")
        boolean systemPreset
) {
}
