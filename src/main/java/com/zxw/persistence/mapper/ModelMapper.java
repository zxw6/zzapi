package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ModelEntity;
import com.zxw.persistence.model.ModelCardView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模型表 Mapper。
 */
/**
 * 模型主数据访问接口。
 * 负责模型列表、状态和公开能力相关的数据操作。
 */
public interface ModelMapper extends BaseMapper<ModelEntity> {

    // 查询某个模型分组下的启用模型列表
    List<ModelCardView> selectActiveModelsByGroup(@Param("groupId") Long groupId);

    default boolean existsByModelCode(String modelCode) {
        // 判断模型编码是否已存在
        return selectCount(Wrappers.<ModelEntity>lambdaQuery()
                .eq(ModelEntity::getModelCode, modelCode)) > 0;
    }

    default int updateActiveById(Long modelId, ModelEntity updateModel) {
        // 更新未删除模型的配置
        return update(updateModel, Wrappers.<ModelEntity>lambdaUpdate()
                .eq(ModelEntity::getId, modelId)
                .eq(ModelEntity::getDeleted, 0));
    }

    default boolean existsActiveById(Long modelId) {
        // 判断模型是否存在且未删除
        return selectCount(Wrappers.<ModelEntity>lambdaQuery()
                .eq(ModelEntity::getId, modelId)
                .eq(ModelEntity::getDeleted, 0)) > 0;
    }

    default Long selectActiveIdByCode(String modelCode) {
        // 根据模型编码查询有效模型ID
        ModelEntity model = selectOne(Wrappers.<ModelEntity>lambdaQuery()
                .eq(ModelEntity::getModelCode, modelCode)
                .eq(ModelEntity::getDeleted, 0)
                .last("limit 1"));
        return model == null ? null : model.getId();
    }

    default Long selectIdByCode(String modelCode) {
        // 根据模型编码查询模型ID，不区分删除状态
        ModelEntity model = selectOne(Wrappers.<ModelEntity>lambdaQuery()
                .eq(ModelEntity::getModelCode, modelCode)
                .last("limit 1"));
        return model == null ? null : model.getId();
    }

    default int updateByIdValue(Long modelId, ModelEntity updateModel) {
        // 仅根据主键更新模型记录
        return update(updateModel, Wrappers.<ModelEntity>lambdaUpdate()
                .eq(ModelEntity::getId, modelId));
    }

    default List<ModelEntity> selectPublicActiveModels() {
        // 查询公开且启用的模型列表
        return selectList(Wrappers.<ModelEntity>lambdaQuery()
                .eq(ModelEntity::getDeleted, 0)
                .eq(ModelEntity::getStatus, "ACTIVE")
                .eq(ModelEntity::getIsPublic, 1)
                .orderByDesc(ModelEntity::getId));
    }
}
