package com.zxw.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationCodeSendRequest(
        @NotBlank(message = "QQ email cannot be blank")
        @Email(message = "Email format is invalid")
        @Pattern(regexp = "^[^\\s@]+@qq\\.com$", message = "Please use a QQ email")
        String email
) {
}
