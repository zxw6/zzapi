package com.zxw.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRegisterRequest(
        @NotBlank(message = "用户名不能为空")
        String username,
        @NotBlank(message = "密码不能为空")
        String password,
        String nickname,
        @NotBlank(message = "QQ邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Pattern(regexp = "^[^\\s@]+@qq\\.com$", message = "请使用QQ邮箱注册")
        String email,
        String phone
) {
}
