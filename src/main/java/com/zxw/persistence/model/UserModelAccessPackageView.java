package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * 用户套餐记录视图对象。
 * 用于承接用户购买套餐后的基础信息与额度配置。
 */
/**
 * 用户套餐访问视图对象。
 * 用于读取用户当前有效套餐及其额度使用情况。
 */
public class UserModelAccessPackageView {

    private Long id;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private LocalDateTime expiresAt;
    private String status;
    private Integer packageDays;
    private BigDecimal dailyQuota;
    private BigDecimal weeklyQuota;
    private BigDecimal monthlyQuota;
}
