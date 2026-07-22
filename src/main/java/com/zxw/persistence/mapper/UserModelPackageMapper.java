package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.UserModelPackageEntity;

import java.time.LocalDateTime;

/**
 * 用户套餐记录 Mapper。
 */
/**
 * 用户套餐记录数据访问接口。
 * 负责套餐删除、过期流转和定向更新。
 */
public interface UserModelPackageMapper extends BaseMapper<UserModelPackageEntity> {

    default int softDeletePackage(Long packageId, Long userId, LocalDateTime updatedAt) {
        // 逻辑删除套餐记录，普通用户场景下会限制 userId
        UserModelPackageEntity updatePackage = new UserModelPackageEntity();
        updatePackage.setStatus("DELETED");
        updatePackage.setUpdatedAt(updatedAt);
        var update = Wrappers.<UserModelPackageEntity>lambdaUpdate()
                .eq(UserModelPackageEntity::getId, packageId)
                .ne(UserModelPackageEntity::getStatus, "DELETED");
        if (userId != null) {
            update.eq(UserModelPackageEntity::getUserId, userId);
        }
        return update(updatePackage, update);
    }

    default int expireActivePackagesBefore(LocalDateTime now) {
        // 把已到期的 ACTIVE 套餐更新为 EXPIRED
        UserModelPackageEntity updateEntity = new UserModelPackageEntity();
        updateEntity.setStatus("EXPIRED");
        updateEntity.setUpdatedAt(now);
        return update(updateEntity, Wrappers.<UserModelPackageEntity>lambdaUpdate()
                .eq(UserModelPackageEntity::getStatus, "ACTIVE")
                .le(UserModelPackageEntity::getExpiresAt, now));
    }

    default int updateByIdAndUserId(Long packageId, Long userId, UserModelPackageEntity updateEntity) {
        // 根据套餐ID更新记录，并可选限制所属用户
        var update = Wrappers.<UserModelPackageEntity>lambdaUpdate()
                .eq(UserModelPackageEntity::getId, packageId);
        if (userId != null) {
            update.eq(UserModelPackageEntity::getUserId, userId);
        }
        return update(updateEntity, update);
    }
}
