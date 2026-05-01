package com.zxw.modules.access.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApiModel("模型套餐总览响应")
/**
 * 模型套餐总览响应对象。
 */
/**
 * 模型套餐总览响应对象。
 * 返回当前用户可用套餐、额度和分组选项信息。
 */
public record ModelAccessSummaryResponse(
        @ApiModelProperty("是否开启套餐限制")
        boolean packageRestrictionEnabled,
        @ApiModelProperty("套餐状态")
        String packageStatus,
        @ApiModelProperty("套餐状态说明")
        String packageStatusText,
        @ApiModelProperty("当前激活分组 id")
        Long activeGroupId,
        @ApiModelProperty("当前激活分组编码")
        String activeGroupCode,
        @ApiModelProperty("当前激活分组名称")
        String activeGroupName,
        @ApiModelProperty("套餐价格")
        BigDecimal packagePrice,
        @ApiModelProperty("日额度")
        BigDecimal dailyQuota,
        @ApiModelProperty("周额度")
        BigDecimal weeklyQuota,
        @ApiModelProperty("月额度")
        BigDecimal monthlyQuota,
        @ApiModelProperty("今日已用额度")
        BigDecimal dailyUsed,
        @ApiModelProperty("本周已用额度")
        BigDecimal weeklyUsed,
        @ApiModelProperty("本月已用额度")
        BigDecimal monthlyUsed,
        @ApiModelProperty("过期时间")
        LocalDateTime expiresAt,
        @ApiModelProperty("剩余天数")
        Long remainingDays,
        @ApiModelProperty("套餐分组选项")
        List<ModelGroupOptionResponse> groups
) {
}
