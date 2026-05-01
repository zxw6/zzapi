package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_sessions")
@ApiModel("Agent会话实体")
/**
 * Agent 会话实体类。
 * 用于保存会话标识、摘要和最近一次响应信息。
 */
/**
 * Agent 会话持久化对象。
 * 记录会话主键、摘要信息和最近一次响应状态。
 */
public class AgentSessionEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("会话ID")
    private Long id;
    @ApiModelProperty("会话标识")
    private String sessionKey;
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("API Key ID")
    private Long apiKeyId;
    @ApiModelProperty("模型编码")
    private String modelCode;
    @ApiModelProperty("工作区路径")
    private String workspaceRoot;
    @ApiModelProperty("摘要内容")
    private String summaryText;
    @ApiModelProperty("摘要消息ID")
    private Long summaryMessageId;
    @ApiModelProperty("最后一次响应ID")
    private String lastResponseId;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
