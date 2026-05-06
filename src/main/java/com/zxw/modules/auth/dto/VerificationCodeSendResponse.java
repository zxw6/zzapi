package com.zxw.modules.auth.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("发送验证码响应")
public record VerificationCodeSendResponse(
        @ApiModelProperty("邮箱")
        String email,
        @ApiModelProperty("有效期秒数")
        long expireSeconds
) {
}
