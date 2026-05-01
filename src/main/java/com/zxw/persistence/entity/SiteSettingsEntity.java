package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_settings")
@ApiModel("站点配置实体")
/**
 * 站点设置实体类。
 * 对应后台维护的站点基础配置。
 */
/**
 * 站点设置实体。
 * 保存站点名称、地址、主题等后台配置项。
 */
public class SiteSettingsEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("配置ID")
    private Long id;
    @ApiModelProperty("配置键")
    private String settingsKey;
    @ApiModelProperty("站点名称")
    private String siteName;
    @ApiModelProperty("管理员邮箱")
    private String adminEmail;
    @ApiModelProperty("站点描述")
    private String siteDescription;
    @ApiModelProperty("站点地址")
    private String baseUrl;
    @ApiModelProperty("页脚文案")
    private String footerText;
    @ApiModelProperty("主题模式")
    private String themeMode;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
