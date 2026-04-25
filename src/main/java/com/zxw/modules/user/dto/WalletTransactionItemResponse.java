package com.zxw.modules.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WalletTransactionItemResponse(
        Long id,
        Long userId,
        String username,
        Long walletId,
        String orderNo,
        String transactionType,
        String direction,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String status,
        String descriptionText,
        LocalDate transactionDate,
        LocalDateTime createdAt
) {
}
