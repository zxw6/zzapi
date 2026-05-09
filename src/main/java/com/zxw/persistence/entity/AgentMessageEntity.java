package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_messages")
@ApiModel("Agent消息实体")
/**
 * Agent 消息实体类。
 * 对应会话中的用户消息、模型消息和工具输出。
 */

public class AgentMessageEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("消息ID")
    private Long id;
    @ApiModelProperty("会话ID")
    private Long sessionId;
    @ApiModelProperty("响应ID")
    private String responseId;
    @ApiModelProperty("角色编码")
    private String roleCode;
    @ApiModelProperty("消息来源类型")
    private String sourceType;
    @ApiModelProperty("消息内容")
    private String contentText;
    @ApiModelProperty("工具名称")
    private String toolName;
    @ApiModelProperty("工具载荷JSON")
    private String toolPayloadJson;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
}
