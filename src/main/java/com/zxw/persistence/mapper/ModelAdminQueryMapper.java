package com.zxw.persistence.mapper;

import com.zxw.persistence.model.ExistingRouteModelView;
import com.zxw.persistence.model.ModelAdminListView;
import com.zxw.persistence.model.ProviderAccessView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模型管理查询 mapper。
 */
public interface ModelAdminQueryMapper {

    /**
     * 查询模型管理列表。
     */
    List<ModelAdminListView> listModels(@Param("admin") boolean admin);

    /**
     * 查询供应商访问权限信息。
     */
    ProviderAccessView selectProviderAccess(@Param("providerId") Long providerId);

    /**
     * 按渠道和上游模型查询是否已有路由绑定。
     */
    ExistingRouteModelView selectExistingRouteModel(@Param("providerId") Long providerId,
                                                    @Param("upstreamModel") String upstreamModel);
}
