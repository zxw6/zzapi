package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserModelAccessPackageView {

    private Long id;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private String packageType;
    private LocalDateTime expiresAt;
    private String status;
    private Integer packageDays;
    private BigDecimal dailyQuota;
    private BigDecimal weeklyQuota;
    private BigDecimal monthlyQuota;
}
