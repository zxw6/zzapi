package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageUsageSummaryView {

    private BigDecimal dailyUsed;
    private BigDecimal weeklyUsed;
    private BigDecimal monthlyUsed;
    private BigDecimal totalUsed;
}
