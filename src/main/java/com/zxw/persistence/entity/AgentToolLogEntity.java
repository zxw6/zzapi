package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_tool_logs")
@ApiModel("Agent工具日志实体")
/**
 * Agent 工具日志实体类。
 * 对应一次工具调用的执行记录。
 */

public class AgentToolLogEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("日志ID")
    private Long id;
    @ApiModelProperty("会话ID")
    private Long sessionId;
    @ApiModelProperty("响应ID")
    private String responseId;
    @ApiModelProperty("步骤序号")
    private Integer stepNo;
    @ApiModelProperty("工具名称")
    private String toolName;
    @ApiModelProperty("标题")
    private String title;
    @ApiModelProperty("是否成功")
    private Integer success;
    @ApiModelProperty("参数JSON")
    private String argumentsJson;
    @ApiModelProperty("结果JSON")
    private String resultJson;
    @ApiModelProperty("文件预览JSON")
    private String filePreviewsJson;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
}
