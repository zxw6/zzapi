package com.zxw.modules.apikey.dto;

public record ApiKeyCreateResponse(
        Long id,
        String plainTextKey
) {
}
