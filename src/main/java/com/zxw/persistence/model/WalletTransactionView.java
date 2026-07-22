package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
/**
 * 钱包流水视图对象。
 * 用于承接钱包充值、消费等流水列表展示数据。
 */
/**
 * 钱包流水视图对象。
 * 用于承接钱包变动列表中的金额和余额变化结果。
 */
public class WalletTransactionView {

    private Long id;
    private Long userId;
    private String username;
    private Long walletId;
    private String orderNo;
    private String transactionType;
    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String status;
    private String descriptionText;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
}
