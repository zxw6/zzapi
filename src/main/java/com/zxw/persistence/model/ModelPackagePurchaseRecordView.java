package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * 套餐购买记录视图对象。
 * 用于后台查看用户套餐购买历史及额度配置。
 */
/**
 * 套餐购买记录视图对象。
 * 用于承接购买记录、额度和状态的联表结果。
 */
public class ModelPackagePurchaseRecordView {

    private Long id;
    private Long userId;
    private String username;
    private Long groupId;
    private String groupCode;
    private String groupName;
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
