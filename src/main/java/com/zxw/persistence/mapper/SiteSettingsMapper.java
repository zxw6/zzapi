package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.SiteSettingsEntity;

/**
 * 站点设置 Mapper。
 */
/**
 * 站点设置数据访问接口。
 * 提供按配置键读取和更新站点配置的能力。
 */
public interface SiteSettingsMapper extends BaseMapper<SiteSettingsEntity> {

    default SiteSettingsEntity selectBySettingsKey(String settingsKey) {
        // 按配置键读取站点配置
        return selectOne(Wrappers.<SiteSettingsEntity>lambdaQuery()
                .eq(SiteSettingsEntity::getSettingsKey, settingsKey)
                .last("limit 1"));
    }

    default boolean existsBySettingsKey(String settingsKey) {
        // 判断指定配置键是否存在
        return selectCount(Wrappers.<SiteSettingsEntity>lambdaQuery()
                .eq(SiteSettingsEntity::getSettingsKey, settingsKey)) > 0;
    }

    default int updateBySettingsKey(String settingsKey, SiteSettingsEntity updateSettings) {
        // 按配置键更新站点配置
        return update(updateSettings, Wrappers.<SiteSettingsEntity>lambdaUpdate()
                .eq(SiteSettingsEntity::getSettingsKey, settingsKey));
    }
}
