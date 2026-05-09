package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ProviderEntity;
import com.zxw.persistence.model.ProviderListView;

import java.time.LocalDateTime;
import java.util.List;

public interface ProviderMapper extends BaseMapper<ProviderEntity> {

    List<ProviderListView> selectProviderList();

    default boolean existsActiveByCode(String providerCode) {
        return selectCount(Wrappers.<ProviderEntity>lambdaQuery()
                .eq(ProviderEntity::getProviderCode, providerCode)
                .eq(ProviderEntity::getDeleted, 0)) > 0;
    }

    default boolean existsActiveById(Long providerId) {
        return selectCount(Wrappers.<ProviderEntity>lambdaQuery()
                .eq(ProviderEntity::getId, providerId)
                .eq(ProviderEntity::getDeleted, 0)) > 0;
    }

    default int updateStatus(Long providerId, String status, LocalDateTime updatedAt) {
        ProviderEntity updateProvider = new ProviderEntity();
        updateProvider.setStatus(status);
        updateProvider.setUpdatedAt(updatedAt);
        return update(updateProvider, Wrappers.<ProviderEntity>lambdaUpdate()
                .eq(ProviderEntity::getId, providerId)
                .eq(ProviderEntity::getDeleted, 0));
    }

    default int softDelete(Long providerId, LocalDateTime updatedAt) {
        ProviderEntity updateProvider = new ProviderEntity();
        updateProvider.setStatus("DISABLED");
        updateProvider.setDeleted(1);
        updateProvider.setUpdatedAt(updatedAt);
        return update(updateProvider, Wrappers.<ProviderEntity>lambdaUpdate()
                .eq(ProviderEntity::getId, providerId)
                .eq(ProviderEntity::getDeleted, 0));
    }
}
