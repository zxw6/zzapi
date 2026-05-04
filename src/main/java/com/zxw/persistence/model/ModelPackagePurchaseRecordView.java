package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ModelPackagePurchaseRecordView {

    private Long id;
    private Long userId;
    private String username;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private String packageType;
    private Integer modelCount;
    private BigDecimal purchasePrice;
    private LocalDateTime startAt;
    private LocalDateTime expiresAt;
    private String status;
    private LocalDateTime createdAt;
    private Integer packageDays;
    private BigDecimal dailyQuota;
    private BigDecimal weeklyQuota;
    private BigDecimal monthlyQuota;
}
