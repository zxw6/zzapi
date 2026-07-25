package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
/**
 * 模型分组价格视图对象。
 * 表示某个模型在特定套餐分组下的价格覆盖配置。
 */
/**
 * 分组模型定价视图对象。
 * 表示某个分组对模型价格的覆盖配置。
 */
public class ModelGroupPricingView {

    private String billingType;
    private BigDecimal promptPrice;
    private BigDecimal cachedPromptPrice;
    private BigDecimal cacheWritePromptPrice;
    private BigDecimal completionPrice;
    private BigDecimal requestPrice;
    private BigDecimal multiplier;
}
