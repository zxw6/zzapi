package com.zxw.modules.dashboard.dto;

import java.math.BigDecimal;

public record DashboardOverviewResponse(
        long userCount,
        long apiKeyCount,
        long providerCount,
        long modelCount,
        long requestCountToday,
        long totalTokensToday,
        long totalTokens7d,
        BigDecimal rechargeAmountToday,
        BigDecimal consumeAmountToday,
        BigDecimal walletBalanceTotal
) {
}
