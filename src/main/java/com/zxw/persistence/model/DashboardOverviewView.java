package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
/**
 * 仪表盘概览视图对象。
 * 用于承接首页概览统计数据。
 */
/**
 * 仪表盘总览视图对象。
 * 用于汇总平台级核心指标查询结果。
 */
public class DashboardOverviewView {

    private Long userCount;
    private Long apiKeyCount;
    private Long providerCount;
    private Long modelCount;
    private Long requestCountToday;
    private Long totalTokensToday;
    private Long totalTokens7d;
    private BigDecimal rechargeAmountToday;
    private BigDecimal consumeAmountToday;
    private BigDecimal walletBalanceTotal;
}
