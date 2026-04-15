package com.zxw.modules.dashboard.dto;

import java.math.BigDecimal;

public record DashboardModelStatResponse(
        String modelCode,
        String upstreamModels,
        long requestCount,
        long totalTokens,
        double avgLatencyMs,
        long totalLatencyMs,
        double successRate,
        BigDecimal userAmount
) {
}
