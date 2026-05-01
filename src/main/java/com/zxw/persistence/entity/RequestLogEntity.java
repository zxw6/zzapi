package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("request_logs")
@ApiModel("请求日志实体")
/**
 * 请求日志实体类。
 * 用于保存模型调用的请求、响应、耗时和计费数据。
 */
/**
 * 请求日志实体。
 * 用于落库每次模型调用的耗时、Token 和金额数据。
 */
public class RequestLogEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("日志ID")
    private Long id;
    @ApiModelProperty("请求ID")
    private String requestId;
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("API Key ID")
    private Long apiKeyId;
    @ApiModelProperty("用户套餐ID")
    private Long userPackageId;
    @ApiModelProperty("模型编码")
    private String modelCode;
    @ApiModelProperty("渠道ID")
    private Long providerId;
    @ApiModelProperty("渠道令牌ID")
    private Long providerTokenId;
    @ApiModelProperty("上游模型")
    private String upstreamModel;
    @ApiModelProperty("请求路径")
    private String requestPath;
    @ApiModelProperty("请求方法")
    private String requestMethod;
    @ApiModelProperty("请求IP")
    private String requestIp;
    @ApiModelProperty("请求体JSON")
    private String requestBodyJson;
    @ApiModelProperty("响应体JSON")
    private String responseBodyJson;
    @ApiModelProperty("输入Token数")
    private Integer promptTokens;
    @ApiModelProperty("输出Token数")
    private Integer completionTokens;
    @ApiModelProperty("总Token数")
    private Integer totalTokens;
    @ApiModelProperty("缓存输入Token数")
    private Integer cachedPromptTokens;
    @ApiModelProperty("用户计费金额")
    private BigDecimal userAmount;
    @ApiModelProperty("成本金额")
    private BigDecimal costAmount;
    @ApiModelProperty("耗时毫秒")
    private Integer latencyMs;
    @ApiModelProperty("是否成功")
    private Integer success;
    @ApiModelProperty("状态码")
    private Integer statusCode;
    @ApiModelProperty("错误信息")
    private String errorMessage;
    @ApiModelProperty("请求日期")
    private LocalDate requestDate;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
}
