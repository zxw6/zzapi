package com.zxw.modules.request.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RequestLogItemResponse(
        String requestId,
        String username,
        String modelCode,
        String upstreamModel,
        Integer statusCode,
        Integer latencyMs,
        Integer totalTokens,
        BigDecimal userAmount,
        BigDecimal costAmount,
        Integer success,
        LocalDateTime createdAt
) {
}
