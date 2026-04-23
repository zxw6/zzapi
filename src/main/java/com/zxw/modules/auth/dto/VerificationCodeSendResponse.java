package com.zxw.modules.auth.dto;

public record VerificationCodeSendResponse(
        String email,
        long expireSeconds,
        String code
) {
}
