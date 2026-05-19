package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ModelRouteEntity;
import com.zxw.persistence.model.GatewayRouteRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ModelRouteMapper extends BaseMapper<ModelRouteEntity> {

    List<GatewayRouteRow> selectRoutesByModelCode(@Param("modelCode") String modelCode);

    List<GatewayRouteRow> selectActiveRoutesByProviderId(@Param("providerId") Long providerId);

    default void deleteByModelId(Long modelId) {
        delete(Wrappers.<ModelRouteEntity>lambdaQuery()
                .eq(ModelRouteEntity::getModelId, modelId));
    }

    default void deleteByModelIdAndProviderId(Long modelId, Long providerId) {
        delete(Wrappers.<ModelRouteEntity>lambdaQuery()
                .eq(ModelRouteEntity::getModelId, modelId)
                .eq(ModelRouteEntity::getProviderId, providerId));
    }

    default int deleteByProviderId(Long providerId) {
        return delete(Wrappers.<ModelRouteEntity>lambdaQuery()
                .eq(ModelRouteEntity::getProviderId, providerId));
    }

    default boolean existsActiveRoute(Long modelId, Long providerId, String upstreamModel) {
        Long count = selectCount(Wrappers.<ModelRouteEntity>lambdaQuery()
                .eq(ModelRouteEntity::getModelId, modelId)
                .eq(ModelRouteEntity::getProviderId, providerId)
                .eq(ModelRouteEntity::getUpstreamModel, upstreamModel)
                .eq(ModelRouteEntity::getStatus, "ACTIVE"));
        return count != null && count > 0;
    }

    default int updateUpstreamModel(Long modelId, Long providerId, String oldUpstreamModel, String newUpstreamModel) {
        return update(Wrappers.<ModelRouteEntity>lambdaUpdate()
                .eq(ModelRouteEntity::getModelId, modelId)
                .eq(ModelRouteEntity::getProviderId, providerId)
                .eq(ModelRouteEntity::getUpstreamModel, oldUpstreamModel)
                .eq(ModelRouteEntity::getRouteType, "PRIMARY")
                .eq(ModelRouteEntity::getStatus, "ACTIVE")
                .set(ModelRouteEntity::getUpstreamModel, newUpstreamModel));
    }
}
