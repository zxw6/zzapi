package com.zxw.modules.user.dto;

public record UserUpdateRequest(
        String nickname,
        String email,
        String phone,
        String roleCode,
        String status,
        String password
) {
}
