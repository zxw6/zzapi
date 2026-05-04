package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardOverviewView {

    private Long userCount;
    private Long apiKeyCount;
    private Long providerCount;
    private Long modelCount;
    private Long requestCountToday;
    private Long totalTokensToday;
    private Long totalTokens7d;
    private Long onlineUserCount;
    private Long todayActiveUserCount;
    private BigDecimal rechargeAmountToday;
    private BigDecimal consumeAmountToday;
    private BigDecimal walletBalanceTotal;
}
