package com.zxw.modules.dashboard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel("控制台概览统计响应")
public record DashboardOverviewResponse(
        @ApiModelProperty("用户总数")
        long userCount,
        @ApiModelProperty("API Key 总数")
        long apiKeyCount,
        @ApiModelProperty("供应商总数")
        long providerCount,
        @ApiModelProperty("模型总数")
        long modelCount,
        @ApiModelProperty("今日请求次数")
        long requestCountToday,
        @ApiModelProperty("今日总 token 数")
        long totalTokensToday,
        @ApiModelProperty("近 7 天总 token 数")
        long totalTokens7d,
        @ApiModelProperty("实时活跃总人数")
        long onlineUserCount,
        @ApiModelProperty("今天使用过的总人数")
        long todayActiveUserCount,
        @ApiModelProperty("今日充值金额")
        BigDecimal rechargeAmountToday,
        @ApiModelProperty("今日消费金额")
        BigDecimal consumeAmountToday,
        @ApiModelProperty("钱包总余额")
        BigDecimal walletBalanceTotal
) {
}
