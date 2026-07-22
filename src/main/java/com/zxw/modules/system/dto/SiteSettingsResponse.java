package com.zxw.modules.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

@ApiModel("站点配置响应")
/**
 * 站点设置响应对象。
 */
/**
 * 站点设置响应对象。
 * 返回当前站点名称、地址和主题等基础配置。
 */
public record SiteSettingsResponse(
        @ApiModelProperty("配置ID")
        Long id,
        @ApiModelProperty("站点名称")
        String siteName,
        @ApiModelProperty("管理员邮箱")
        String adminEmail,
        @ApiModelProperty("站点描述")
        String siteDescription,
        @ApiModelProperty("接口基础地址")
        String baseUrl,
        @ApiModelProperty("页脚文字")
        String footerText,
        @ApiModelProperty("主题模式")
        String themeMode,
        @ApiModelProperty("更新时间")
        LocalDateTime updatedAt
) {
}
