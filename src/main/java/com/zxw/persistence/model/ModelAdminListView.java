package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * 后台模型列表视图对象。
 * 用于承接模型、分组、渠道联表后的展示数据。
 */
/**
 * 后台模型列表视图对象。
 * 用于展示模型、分组、渠道和价格等综合信息。
 */
public class ModelAdminListView {

    private Long id;
    private Long bindingId;
    private String modelCode;
    private String modelName;
    private String modelType;
    private String billingType;
    private BigDecimal promptPrice;
    private BigDecimal cachedPromptPrice;
    private BigDecimal cacheWritePromptPrice;
    private BigDecimal completionPrice;
    private BigDecimal requestPrice;
    private BigDecimal multiplier;
    private Integer isPublic;
    private String status;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private Long providerId;
    private String providerName;
    private String providerType;
    private String upstreamModel;
    private LocalDateTime createdAt;
}
