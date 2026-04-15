package com.zxw.modules.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        String username,
        @NotBlank(message = "密码不能为空")
        String password,
        String nickname,
        String email,
        String phone,
        String roleCode,
        @DecimalMin(value = "0.00", message = "初始余额不能小于 0")
        BigDecimal initialBalance
) {
}
