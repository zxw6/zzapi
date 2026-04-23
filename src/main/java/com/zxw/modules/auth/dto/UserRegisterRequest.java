package com.zxw.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRegisterRequest(
        @NotBlank(message = "Username cannot be blank")
        String username,
        @NotBlank(message = "Password cannot be blank")
        String password,
        String nickname,
        @NotBlank(message = "QQ email cannot be blank")
        @Email(message = "Email format is invalid")
        @Pattern(regexp = "^[^\\s@]+@qq\\.com$", message = "Please use a QQ email")
        String email,
        String phone,
        @NotBlank(message = "Verification code cannot be blank")
        @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
        String verificationCode
) {
}
