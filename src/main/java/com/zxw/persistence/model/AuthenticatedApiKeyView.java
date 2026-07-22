package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuthenticatedApiKeyView {

    private Long id;
    private Long userId;
    private String username;
    private String roleCode;
    private String secretHash;
    private String status;
    private Long userPackageId;
    private String packageName;
    private String packageType;
    private Long modelGroupId;
    private String groupCode;
    private String groupName;
    private BigDecimal totalQuota;
    private BigDecimal usedQuota;
    private LocalDateTime expiresAt;
    private BigDecimal balance;
    private Integer packageRestrictionEnabled;
    private Integer maxConcurrentRequests;
    private Integer maxConcurrentStreams;
}
