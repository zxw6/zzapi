package com.zxw.modules.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardTrendPointResponse(
        LocalDate statDate,
        long requestCount,
        long successCount,
        long totalTokens,
        BigDecimal userAmount,
        BigDecimal costAmount
) {
}
