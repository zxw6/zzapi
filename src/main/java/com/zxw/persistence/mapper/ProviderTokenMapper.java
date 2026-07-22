package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ProviderTokenEntity;

import java.time.LocalDateTime;

public interface ProviderTokenMapper extends BaseMapper<ProviderTokenEntity> {

    default int softDeleteByProviderId(Long providerId, LocalDateTime updatedAt) {
        ProviderTokenEntity updateToken = new ProviderTokenEntity();
        updateToken.setStatus("DISABLED");
        updateToken.setDeleted(1);
        updateToken.setUpdatedAt(updatedAt);
        return update(updateToken, Wrappers.<ProviderTokenEntity>lambdaUpdate()
                .eq(ProviderTokenEntity::getProviderId, providerId)
                .eq(ProviderTokenEntity::getDeleted, 0));
    }
}
