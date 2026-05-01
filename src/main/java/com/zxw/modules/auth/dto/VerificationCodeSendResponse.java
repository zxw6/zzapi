package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("发送验证码响应")
/**
 * 验证码发送响应对象。
 */
/**
 * 验证码发送响应对象。
 * 返回验证码的目标邮箱和有效期信息。
 */
public record VerificationCodeSendResponse(
        @ApiModelProperty("邮箱")
        String email,
        @ApiModelProperty("有效期秒数")
        long expireSeconds,
        @ApiModelProperty("验证码")
        String code
) {
}
