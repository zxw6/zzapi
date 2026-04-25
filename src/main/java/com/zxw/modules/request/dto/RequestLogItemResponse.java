package com.zxw.modules.request.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RequestLogItemResponse(
        String requestId,
        String username,
        String modelCode,
        String upstreamModel,
        String packageName,
        BigDecimal multiplier,
        Integer statusCode,
        Integer latencyMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Integer cachedPromptTokens,
        BigDecimal userAmount,
        BigDecimal costAmount,
        Integer success,
        LocalDateTime createdAt
) {
}
