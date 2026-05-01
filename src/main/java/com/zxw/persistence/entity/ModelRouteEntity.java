package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("model_routes")
@ApiModel("模型路由实体")
/**
 * 模型路由实体类。
 * 用于定义公开模型与上游渠道模型之间的映射。
 */
/**
 * 模型路由实体。
 * 描述平台模型到上游渠道模型的映射关系。
 */
public class ModelRouteEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("路由ID")
    private Long id;
    @ApiModelProperty("模型ID")
    private Long modelId;
    @ApiModelProperty("渠道ID")
    private Long providerId;
    @ApiModelProperty("渠道令牌ID")
    private Long providerTokenId;
    @ApiModelProperty("上游模型名")
    private String upstreamModel;
    @ApiModelProperty("路由类型")
    private String routeType;
    @ApiModelProperty("优先级")
    private Integer priorityNo;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
