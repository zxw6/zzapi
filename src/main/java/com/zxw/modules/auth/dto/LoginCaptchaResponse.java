package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("登录图形验证码响应")
/**
 * 登录图形验证码响应对象。
 * 返回本次登录需要使用的验证码标识、图片内容和有效期。
 */
public record LoginCaptchaResponse(
        @ApiModelProperty("验证码ID")
        String captchaId,
        @ApiModelProperty("图形验证码Base64数据")
        String imageBase64,
        @ApiModelProperty("有效期秒数")
        long expireSeconds
) {
}
