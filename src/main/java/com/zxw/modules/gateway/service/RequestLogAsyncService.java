package com.zxw.modules.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxw.persistence.entity.RequestLogEntity;
import com.zxw.persistence.mapper.RequestLogMapper;
import com.zxw.persistence.mapper.UsageDailyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class RequestLogAsyncService {

    private static final Logger log = LoggerFactory.getLogger(RequestLogAsyncService.class);

    private final RequestLogMapper requestLogMapper;
    private final UsageDailyMapper usageDailyMapper;
    private final ObjectMapper objectMapper;

    public RequestLogAsyncService(RequestLogMapper requestLogMapper,
                                  UsageDailyMapper usageDailyMapper,
                                  ObjectMapper objectMapper) {
        this.requestLogMapper = requestLogMapper;
        this.usageDailyMapper = usageDailyMapper;
        this.objectMapper = objectMapper;
    }

    @Async("gatewayLogTaskExecutor")
    @Transactional
    public void persistRequestLogAndUsage(RequestLogEntity requestLog, UsageDailyIncrement usage) {
        persistRequestLog(requestLog);
        if (usage != null) {
            try {
                usageDailyMapper.upsert(
                        usage.requestDate(),
                        usage.userId(),
                        usage.modelCode(),
                        usage.providerId(),
                        usage.successCount(),
                        usage.totalTokens(),
                        usage.userAmount(),
                        usage.costAmount()
                );
            } catch (Exception ex) {
                log.warn("Failed to persist usage daily for requestId={}, model={}: {}",
                        requestLog == null ? null : requestLog.getRequestId(),
                        usage.modelCode(),
                        ex.getMessage());
            }
        }
    }

    @Async("gatewayLogTaskExecutor")
    public void persistFailedRequestLog(RequestLogEntity requestLog) {
        persistRequestLog(requestLog);
    }

    private void persistRequestLog(RequestLogEntity requestLog) {
        if (requestLog == null) {
            return;
        }
        try {
            requestLog.setRequestBodyJson(normalizeJson(requestLog.getRequestBodyJson()));
            requestLog.setResponseBodyJson(normalizeJson(requestLog.getResponseBodyJson()));
            requestLogMapper.insert(requestLog);
        } catch (Exception ex) {
            log.warn("Failed to persist request log for requestId={}, model={}: {}",
                    requestLog.getRequestId(), requestLog.getModelCode(), ex.getMessage());
        }
    }

    private String normalizeJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json == null) {
                return null;
            }
            return objectMapper.writeValueAsString(json);
        } catch (Exception ignored) {
            return "{\"rawText\":" + jsonStringLiteral(truncateForLog(body, 2000)) + "}";
        }
    }

    private String truncateForLog(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String jsonStringLiteral(String value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "\"log-body-unavailable\"";
        }
    }

    public record UsageDailyIncrement(
            LocalDate requestDate,
            Long userId,
            String modelCode,
            Long providerId,
            int successCount,
            int totalTokens,
            BigDecimal userAmount,
            BigDecimal costAmount
    ) {
    }
}
