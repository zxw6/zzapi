package com.zxw.modules.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserListItemResponse(
        Long id,
        String username,
        String nickname,
        String roleCode,
        String status,
        String email,
        String phone,
        BigDecimal balance,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
