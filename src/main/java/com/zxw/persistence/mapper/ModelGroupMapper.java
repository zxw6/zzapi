package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ModelGroupEntity;

import java.time.LocalDateTime;

/**
 * 模型套餐分组 Mapper。
 */
/**
 * 模型分组数据访问接口。
 * 负责分组主数据查询、重复校验和状态更新。
 */
public interface ModelGroupMapper extends BaseMapper<ModelGroupEntity> {

    default boolean existsByGroupCode(String groupCode) {
        Long count = selectCount(Wrappers.<ModelGroupEntity>lambdaQuery()
                .eq(ModelGroupEntity::getGroupCode, groupCode));
        return count != null && count > 0;
    }

    default ModelGroupEntity selectByGroupCode(String groupCode) {
        return selectOne(Wrappers.<ModelGroupEntity>lambdaQuery()
                .eq(ModelGroupEntity::getGroupCode, groupCode)
                .last("limit 1"));
    }

    default int updateStatusById(Long groupId, String status, LocalDateTime updatedAt) {
        ModelGroupEntity updateEntity = new ModelGroupEntity();
        updateEntity.setStatus(status);
        updateEntity.setUpdatedAt(updatedAt);
        return update(updateEntity, Wrappers.<ModelGroupEntity>lambdaUpdate()
                .eq(ModelGroupEntity::getId, groupId));
    }

    default int updateByIdValue(Long groupId, ModelGroupEntity updateEntity) {
        return update(updateEntity, Wrappers.<ModelGroupEntity>lambdaUpdate()
                .eq(ModelGroupEntity::getId, groupId));
    }

    default Long countByGroupCode(String groupCode) {
        return selectCount(Wrappers.<ModelGroupEntity>lambdaQuery()
                .eq(ModelGroupEntity::getGroupCode, groupCode));
    }
}
