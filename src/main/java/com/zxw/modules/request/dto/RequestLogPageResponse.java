package com.zxw.modules.request.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel("请求日志分页响应")
/**
 * 请求日志分页响应对象。
 * 用于返回分页后的请求日志列表和分页元数据。
 */
public record RequestLogPageResponse<T>(
        @ApiModelProperty("当前页码")
        int page,
        @ApiModelProperty("每页条数")
        int pageSize,
        @ApiModelProperty("总记录数")
        long total,
        @ApiModelProperty("总页数")
        long totalPages,
        @ApiModelProperty("是否有上一页")
        boolean hasPrevious,
        @ApiModelProperty("是否有下一页")
        boolean hasNext,
        @ApiModelProperty("当前页数据")
        List<T> records
) {
}
