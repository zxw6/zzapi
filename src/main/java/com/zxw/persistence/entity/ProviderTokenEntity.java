package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("provider_tokens")
@ApiModel("渠道令牌实体")
/**
 * 渠道令牌实体类。
 * 用于保存上游令牌、限流和余额等信息。
 */
/**
 * 渠道令牌实体。
 * 保存渠道访问令牌、限流权重和余额信息。
 */
public class ProviderTokenEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("令牌ID")
    private Long id;
    @ApiModelProperty("所属渠道ID")
    private Long providerId;
    @ApiModelProperty("令牌名称")
    private String tokenName;
    @ApiModelProperty("加密后的令牌值")
    private String tokenValueEncrypted;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("权重")
    private Integer weightNo;
    @ApiModelProperty("每分钟请求限制")
    private Integer rpmLimit;
    @ApiModelProperty("每分钟Token限制")
    private Integer tpmLimit;
    @ApiModelProperty("当前余额")
    private BigDecimal currentBalance;
    @ApiModelProperty("删除标记")
    private Integer deleted;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
