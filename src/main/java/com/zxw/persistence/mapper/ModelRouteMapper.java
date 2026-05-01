package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ModelRouteEntity;
import com.zxw.persistence.model.GatewayRouteRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模型路由 Mapper。
 */
/**
 * 模型路由数据访问接口。
 * 用于查询路由映射并维护模型到渠道的绑定关系。
 */
public interface ModelRouteMapper extends BaseMapper<ModelRouteEntity> {

    // 按模型编码查询可用路由列表
    List<GatewayRouteRow> selectRoutesByModelCode(@Param("modelCode") String modelCode);

    default void deleteByModelId(Long modelId) {
        // 删除某个模型下的全部路由
        delete(Wrappers.<ModelRouteEntity>lambdaQuery()
                .eq(ModelRouteEntity::getModelId, modelId));
    }

    default void deleteByModelIdAndProviderId(Long modelId, Long providerId) {
        // 删除某个模型在指定渠道下的路由
        delete(Wrappers.<ModelRouteEntity>lambdaQuery()
                .eq(ModelRouteEntity::getModelId, modelId)
                .eq(ModelRouteEntity::getProviderId, providerId));
    }
}
