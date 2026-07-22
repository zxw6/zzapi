package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserModelAccessGroupView {

    private Long id;
    private String groupCode;
    private String groupName;
    private String packageType;
    private BigDecimal salePrice;
    private Integer packageDays;
    private BigDecimal dailyQuota;
    private BigDecimal weeklyQuota;
    private BigDecimal monthlyQuota;
    private Integer modelCount;
    private String remark;
}
