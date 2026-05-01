package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
/**
 * 用户模型套餐分组视图对象。
 * 用于承接套餐分组基础信息及其模型数量。
 */
/**
 * 用户模型分组访问视图对象。
 * 用于返回分组套餐的价格、额度和启用状态信息。
 */
public class UserModelAccessGroupView {

    private Long id;
    private String groupCode;
    private String groupName;
    private BigDecimal salePrice;
    private Integer packageDays;
    private BigDecimal dailyQuota;
    private BigDecimal weeklyQuota;
    private BigDecimal monthlyQuota;
    private Integer modelCount;
    private String remark;
}
