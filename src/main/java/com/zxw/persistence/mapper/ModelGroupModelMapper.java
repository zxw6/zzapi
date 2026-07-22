package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ModelGroupModelEntity;
import com.zxw.persistence.model.ModelGroupPricingView;
import org.apache.ibatis.annotations.Param;

/**
 * 模型与分组绑定 Mapper。
 */
/**
 * 模型分组绑定数据访问接口。
 * 提供模型与套餐分组关系的查询和维护能力。
 */
public interface ModelGroupModelMapper extends BaseMapper<ModelGroupModelEntity> {

    // 查询模型在某个套餐分组下的价格覆盖配置
    ModelGroupPricingView selectGroupPricing(@Param("groupId") Long groupId, @Param("modelId") Long modelId);

    // 插入模型与分组绑定，忽略重复绑定
    int insertIgnoreBinding(@Param("groupId") Long groupId, @Param("modelId") Long modelId);

    default boolean existsBinding(Long modelId, Long groupId) {
        // 判断模型和分组之间是否已存在绑定关系
        Long count = selectCount(Wrappers.<ModelGroupModelEntity>lambdaQuery()
                .eq(ModelGroupModelEntity::getModelId, modelId)
                .eq(ModelGroupModelEntity::getGroupId, groupId));
        return count != null && count > 0;
    }

    default void deleteByModelId(Long modelId) {
        // 删除某个模型下的全部分组绑定
        delete(Wrappers.<ModelGroupModelEntity>lambdaQuery()
                .eq(ModelGroupModelEntity::getModelId, modelId));
    }

    default void deleteByIdValue(Long bindingId) {
        // 按绑定主键删除记录
        deleteById(bindingId);
    }

    default void deleteByModelIdAndGroupId(Long modelId, Long groupId) {
        // 删除某个模型在指定分组下的绑定
        delete(Wrappers.<ModelGroupModelEntity>lambdaQuery()
                .eq(ModelGroupModelEntity::getModelId, modelId)
                .eq(ModelGroupModelEntity::getGroupId, groupId));
    }
}
