package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * API Key 列表查询视图对象。
 * 用于承接 API Key 列表页需要展示的用户、套餐、额度信息。
 */
/**
 * API Key 列表视图对象。
 * 承接后台密钥列表页需要展示的用户、套餐和额度信息。
 */
public class ApiKeyListView {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String accessKey;
    private String status;
    private Long modelPackageId;
    private String modelPackageName;
    private Long modelGroupId;
    private String modelGroupName;
    private BigDecimal totalQuota;
    private BigDecimal usedQuota;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}
