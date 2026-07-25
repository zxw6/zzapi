package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.RequestLogEntity;

import java.time.LocalDateTime;

/**
 * Request log mapper.
 */
public interface RequestLogMapper extends BaseMapper<RequestLogEntity> {

    default int deleteOlderThan(LocalDateTime cutoff) {
        return delete(Wrappers.<RequestLogEntity>lambdaQuery()
                .lt(RequestLogEntity::getCreatedAt, cutoff));
    }

    default int deleteByUserId(Long userId) {
        return delete(Wrappers.<RequestLogEntity>lambdaQuery()
                .eq(RequestLogEntity::getUserId, userId));
    }

    default int deleteAllLogs() {
        return delete(Wrappers.<RequestLogEntity>lambdaQuery()
                .isNotNull(RequestLogEntity::getId));
    }
}
