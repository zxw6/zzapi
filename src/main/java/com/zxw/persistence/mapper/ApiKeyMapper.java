package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.ApiKeyEntity;
import com.zxw.persistence.model.AuthenticatedApiKeyView;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key Mapper。
 */
/**
 * API Key 数据访问接口。
 * 封装鉴权命中、额度累计和状态维护等操作。
 */
public interface ApiKeyMapper extends BaseMapper<ApiKeyEntity> {

    // 按访问前缀查询可能命中的 API Key 列表
    List<AuthenticatedApiKeyView> selectAuthenticatedByAccessKey(@Param("accessKey") String accessKey);

    // 累加 API Key 已使用额度
    int incrementUsedQuota(@Param("apiKeyId") Long apiKeyId, @Param("amount") java.math.BigDecimal amount);

    default int updateLastUsedAt(Long apiKeyId, LocalDateTime now) {
        // 更新 API Key 最近一次使用时间
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setLastUsedAt(now);
        apiKey.setUpdatedAt(now);
        return update(apiKey, Wrappers.<ApiKeyEntity>lambdaUpdate()
                .eq(ApiKeyEntity::getId, apiKeyId));
    }

    default int updateStatus(Long id, Long userId, String status, LocalDateTime updatedAt) {
        // 更新 API Key 状态，普通用户场景下会额外限制 userId
        ApiKeyEntity updateEntity = new ApiKeyEntity();
        updateEntity.setStatus(status);
        updateEntity.setUpdatedAt(updatedAt);
        var update = Wrappers.<ApiKeyEntity>lambdaUpdate()
                .eq(ApiKeyEntity::getId, id)
                .eq(ApiKeyEntity::getDeleted, 0);
        if (userId != null) {
            update.eq(ApiKeyEntity::getUserId, userId);
        }
        return update(updateEntity, update);
    }

    default int softDelete(Long id, Long userId, LocalDateTime updatedAt) {
        // 对 API Key 做逻辑删除
        ApiKeyEntity updateEntity = new ApiKeyEntity();
        updateEntity.setDeleted(1);
        updateEntity.setUpdatedAt(updatedAt);
        var update = Wrappers.<ApiKeyEntity>lambdaUpdate()
                .eq(ApiKeyEntity::getId, id)
                .eq(ApiKeyEntity::getDeleted, 0);
        if (userId != null) {
            update.eq(ApiKeyEntity::getUserId, userId);
        }
        return update(updateEntity, update);
    }

    default int disableByUserPackageId(Long packageId, Long userId, LocalDateTime updatedAt) {
        // 套餐失效时批量禁用关联 API Key
        ApiKeyEntity updateEntity = new ApiKeyEntity();
        updateEntity.setStatus("DISABLED");
        updateEntity.setUpdatedAt(updatedAt);
        var update = Wrappers.<ApiKeyEntity>lambdaUpdate()
                .eq(ApiKeyEntity::getUserPackageId, packageId)
                .eq(ApiKeyEntity::getDeleted, 0);
        if (userId != null) {
            update.eq(ApiKeyEntity::getUserId, userId);
        }
        return update(updateEntity, update);
    }

    default void deleteByUserId(Long userId) {
        // 删除指定用户下的所有 API Key
        delete(Wrappers.<ApiKeyEntity>lambdaQuery()
                .eq(ApiKeyEntity::getUserId, userId));
    }
}
