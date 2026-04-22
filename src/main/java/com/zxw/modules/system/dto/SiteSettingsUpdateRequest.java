package com.zxw.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SiteSettingsUpdateRequest(
        @NotBlank(message = "站点名称不能为空")
        @Size(max = 128, message = "站点名称长度不能超过 128 个字符")
        String siteName,

        @NotBlank(message = "管理员邮箱不能为空")
        @Email(message = "管理员邮箱格式不正确")
        @Size(max = 128, message = "管理员邮箱长度不能超过 128 个字符")
        String adminEmail,

        @Size(max = 255, message = "站点描述长度不能超过 255 个字符")
        String siteDescription,

        @NotBlank(message = "接口基础地址不能为空")
        @Pattern(regexp = "https?://.+", message = "接口基础地址必须以 http:// 或 https:// 开头")
        @Size(max = 255, message = "接口基础地址长度不能超过 255 个字符")
        String baseUrl,

        @Size(max = 255, message = "页脚文字长度不能超过 255 个字符")
        String footerText,

        @NotBlank(message = "界面主题不能为空")
        @Pattern(regexp = "(?i)LIGHT|DARK", message = "界面主题仅支持 LIGHT 或 DARK")
        String themeMode
) {
}
