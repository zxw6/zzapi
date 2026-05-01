package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ProviderEntity;
import com.zxw.persistence.model.ProviderListView;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道配置 Mapper。
 */
/**
 * 渠道数据访问接口。
 * 负责渠道列表查询和渠道状态维护。
 */
public interface ProviderMapper extends BaseMapper<ProviderEntity> {

    // 查询渠道列表联表结果
    List<ProviderListView> selectProviderList();

    default boolean existsActiveByCode(String providerCode) {
        // 判断渠道编码是否已存在且未删除
        return selectCount(Wrappers.<ProviderEntity>lambdaQuery()
                .eq(ProviderEntity::getProviderCode, providerCode)
                .eq(ProviderEntity::getDeleted, 0)) > 0;
    }

    default int updateStatus(Long providerId, String status, LocalDateTime updatedAt) {
        // 更新渠道状态
        ProviderEntity updateProvider = new ProviderEntity();
        updateProvider.setStatus(status);
        updateProvider.setUpdatedAt(updatedAt);
        return update(updateProvider, Wrappers.<ProviderEntity>lambdaUpdate()
                .eq(ProviderEntity::getId, providerId)
                .eq(ProviderEntity::getDeleted, 0));
    }
}
