package com.zxw.common.security;

public record JwtUser(
        Long userId,
        String username,
        String roleCode
) {
}
