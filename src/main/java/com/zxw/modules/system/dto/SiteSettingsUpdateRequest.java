package com.zxw.modules.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ApiModel("更新站点配置请求")
/**
 * 站点设置更新请求对象。
 */
/**
 * 站点设置更新请求对象。
 * 用于保存后台展示的站点基础配置。
 */
public record SiteSettingsUpdateRequest(
        @ApiModelProperty(value = "站点名称", required = true)
        @NotBlank(message = "站点名称不能为空")
        @Size(max = 128, message = "站点名称长度不能超过 128 个字符")
        String siteName,

        @ApiModelProperty(value = "管理员邮箱", required = true)
        @NotBlank(message = "管理员邮箱不能为空")
        @Email(message = "管理员邮箱格式不正确")
        @Size(max = 128, message = "管理员邮箱长度不能超过 128 个字符")
        String adminEmail,

        @ApiModelProperty("站点描述")
        @Size(max = 255, message = "站点描述长度不能超过 255 个字符")
        String siteDescription,

        @ApiModelProperty(value = "接口基础地址", required = true)
        @NotBlank(message = "接口基础地址不能为空")
        @Pattern(regexp = "https?://.+", message = "接口基础地址必须以 http:// 或 https:// 开头")
        @Size(max = 255, message = "接口基础地址长度不能超过 255 个字符")
        String baseUrl,

        @ApiModelProperty("页脚文字")
        @Size(max = 255, message = "页脚文字长度不能超过 255 个字符")
        String footerText,

        @ApiModelProperty(value = "界面主题", required = true)
        @NotBlank(message = "界面主题不能为空")
        @Pattern(regexp = "(?i)LIGHT|DARK", message = "界面主题仅支持 LIGHT 或 DARK")
        String themeMode
) {
}
