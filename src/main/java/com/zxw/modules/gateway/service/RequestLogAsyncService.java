package com.zxw.modules.gateway.service;

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

    public RequestLogAsyncService(RequestLogMapper requestLogMapper,
                                  UsageDailyMapper usageDailyMapper) {
        this.requestLogMapper = requestLogMapper;
        this.usageDailyMapper = usageDailyMapper;
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
                if (usage.userPackageId() != null) {
                    usageDailyMapper.upsertPackage(
                            usage.requestDate(),
                            usage.userId(),
                            usage.userPackageId(),
                            usage.successCount(),
                            usage.totalTokens(),
                            usage.userAmount(),
                            usage.costAmount()
                    );
                }
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
            requestLog.setRequestBodyJson(null);
            requestLog.setResponseBodyJson(null);
            requestLogMapper.insert(requestLog);
        } catch (Exception ex) {
            log.warn("Failed to persist request log for requestId={}, model={}: {}",
                    requestLog.getRequestId(), requestLog.getModelCode(), ex.getMessage());
        }
    }

    public record UsageDailyIncrement(
            LocalDate requestDate,
            Long userId,
            Long userPackageId,
            String modelCode,
            Long providerId,
            int successCount,
            int totalTokens,
            BigDecimal userAmount,
            BigDecimal costAmount
    ) {
    }
}
