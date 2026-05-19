package com.zxw.modules.gateway.service;

import com.zxw.persistence.entity.TransactionEntity;
import com.zxw.persistence.mapper.ApiKeyMapper;
import com.zxw.persistence.mapper.TransactionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class GatewayAccountingAsyncService {

    private static final Logger log = LoggerFactory.getLogger(GatewayAccountingAsyncService.class);

    private final TransactionMapper transactionMapper;
    private final ApiKeyMapper apiKeyMapper;

    public GatewayAccountingAsyncService(TransactionMapper transactionMapper,
                                         ApiKeyMapper apiKeyMapper) {
        this.transactionMapper = transactionMapper;
        this.apiKeyMapper = apiKeyMapper;
    }

    @Async("gatewayLogTaskExecutor")
    @Transactional
    public void persistConsumeTransactionAndQuota(TransactionEntity transaction,
                                                  Long apiKeyId,
                                                  BigDecimal userAmount) {
        if (transaction != null) {
            try {
                transactionMapper.insert(transaction);
            } catch (Exception ex) {
                log.warn("Failed to persist consume transaction for userId={}, orderNo={}: {}",
                        transaction.getUserId(), transaction.getOrderNo(), ex.getMessage());
            }
        }
        incrementUsedQuota(apiKeyId, userAmount);
    }

    @Async("gatewayLogTaskExecutor")
    public void incrementUsedQuota(Long apiKeyId, BigDecimal userAmount) {
        if (apiKeyId == null || userAmount == null || userAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            apiKeyMapper.incrementUsedQuota(apiKeyId, userAmount);
        } catch (Exception ex) {
            log.warn("Failed to increment API key used quota for apiKeyId={}: {}", apiKeyId, ex.getMessage());
        }
    }
}
