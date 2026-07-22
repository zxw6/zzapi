package com.zxw.persistence.mapper;

import com.zxw.persistence.model.ExistingRouteModelView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Antigravity 预置模型查询 mapper。
 */
public interface AntigravityQueryMapper {

    /**
     * 查询所有 Antigravity 供应商 id。
     */
    List<Long> selectAntigravityProviderIds();

    /**
     * 统计指定供应商是否为 Antigravity。
     */
    Integer countAntigravityProviders(@Param("providerId") Long providerId);

    /**
     * 按渠道路由查找已存在的模型。
     */
    ExistingRouteModelView selectModelByProviderRoute(@Param("providerId") Long providerId,
                                                      @Param("upstreamModel") String upstreamModel);
}
