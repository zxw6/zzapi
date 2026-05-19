package com.zxw.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * 用户列表视图对象。
 * 用于后台用户列表和用户详情查询结果承接。
 */
/**
 * 用户列表视图对象。
 * 承接后台用户列表和详情页所需的基础数据。
 */
public class UserListView {

    private Long id;
    private String username;
    private String nickname;
    private String roleCode;
    private String status;
    private String email;
    private String phone;
    private BigDecimal balance;
    private Integer maxConcurrentRequests;
    private Integer maxConcurrentStreams;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
