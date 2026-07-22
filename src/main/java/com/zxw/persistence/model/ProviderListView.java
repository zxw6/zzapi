package com.zxw.persistence.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 渠道列表查询视图对象。
 * 用于承接渠道列表联表查询结果。
 */
/**
 * 渠道列表视图对象。
 * 用于承接渠道列表页的联表展示结果。
 */
public class ProviderListView {

    private Long id;
    private String providerCode;
    private String providerName;
    private String baseUrl;
    private String providerType;
    private String status;
    private Integer priorityNo;
    private Integer timeoutMs;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}
