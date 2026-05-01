package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.RequestLogEntity;

import java.time.LocalDateTime;

/**
 * 请求日志 Mapper。
 */
/**
 * 请求日志数据访问接口。
 * 用于维护调用日志的清理和基础持久化能力。
 */
public interface RequestLogMapper extends BaseMapper<RequestLogEntity> {

    default int deleteOlderThan(LocalDateTime cutoff) {
        // 删除早于指定时间的请求日志
        return delete(Wrappers.<RequestLogEntity>lambdaQuery()
                .lt(RequestLogEntity::getCreatedAt, cutoff));
    }
}
