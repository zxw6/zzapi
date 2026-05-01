package com.zxw.modules.apikey.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("创建API Key响应")
/**
 * API Key 创建响应对象。
 */
/**
 * API Key 创建响应对象。
 * 返回新建密钥的主键和仅展示一次的明文密钥。
 */
public record ApiKeyCreateResponse(
        @ApiModelProperty("主键ID")
        Long id,
        @ApiModelProperty("明文密钥")
        String plainTextKey
) {
}
