package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * API Key 鉴权查询视图对象。
 * 用于承接鉴权时联表查询出来的用户、套餐、额度等信息。
 */
/**
 * API Key 鉴权视图对象。
 * 用于承接鉴权时联表查询出的套餐、余额和权限数据。
 */
public class AuthenticatedApiKeyView {

    private Long id;
    private Long userId;
    private String username;
    private String roleCode;
    private String secretHash;
    private String status;
    private Long userPackageId;
    private String packageName;
    private Long modelGroupId;
    private String groupCode;
    private String groupName;
    private BigDecimal totalQuota;
    private BigDecimal usedQuota;
    private LocalDateTime expiresAt;
    private BigDecimal balance;
    private Integer packageRestrictionEnabled;
}
