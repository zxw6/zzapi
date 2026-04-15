package com.zxw.modules.auth.dto;

import java.math.BigDecimal;

public record AdminLoginResponse(
        String token,
        Long userId,
        String username,
        String nickname,
        String roleCode,
        BigDecimal balance
) {
}
