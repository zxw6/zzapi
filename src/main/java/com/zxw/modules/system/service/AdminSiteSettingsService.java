package com.zxw.modules.system.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.modules.system.dto.SiteSettingsResponse;
import com.zxw.modules.system.dto.SiteSettingsUpdateRequest;
import com.zxw.persistence.entity.SiteSettingsEntity;
import com.zxw.persistence.mapper.SiteSettingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
/**
 * 站点配置服务。
 * 负责站点基础信息的查询、更新以及默认配置初始化。
 */
public class AdminSiteSettingsService {

    private static final String DEFAULT_SETTINGS_KEY = "DEFAULT";
    private static final String DEFAULT_SITE_NAME = "API Hub";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@apihub.io";
    private static final String DEFAULT_SITE_DESCRIPTION = "Enterprise AI API gateway management platform";
    private static final String DEFAULT_BASE_URL = "https://api.yourdomain.com";
    private static final String DEFAULT_FOOTER_TEXT = "Powered by API Hub";
    private static final String DEFAULT_THEME_MODE = "LIGHT";

    private final SiteSettingsMapper siteSettingsMapper;

    public AdminSiteSettingsService(SiteSettingsMapper siteSettingsMapper) {
        this.siteSettingsMapper = siteSettingsMapper;
    }

    /**
     * 查询当前站点设置。
     */
    public SiteSettingsResponse getSiteSettings() {
        AdminContext.requireAdmin();
        // 查询前先确保默认配置存在
        initializeDefaults();

        SiteSettingsEntity settings = siteSettingsMapper.selectBySettingsKey(DEFAULT_SETTINGS_KEY);
        if (settings == null) {
            throw new BusinessException("Site settings not found");
        }

        // 转成对外返回的响应结构
        return new SiteSettingsResponse(
                settings.getId(),
                settings.getSiteName(),
                settings.getAdminEmail(),
                settings.getSiteDescription(),
                settings.getBaseUrl(),
                settings.getFooterText(),
                settings.getThemeMode(),
                settings.getUpdatedAt()
        );
    }

    @Transactional
    /**
     * 更新站点设置。
     */
    public void updateSiteSettings(SiteSettingsUpdateRequest request) {
        AdminContext.requireAdmin();
        // 更新前确保配置记录已经存在
        initializeDefaults();

        // 规范化前端传入的数据后再落库
        SiteSettingsEntity updateSettings = new SiteSettingsEntity();
        updateSettings.setSiteName(normalize(request.siteName()));
        updateSettings.setAdminEmail(normalize(request.adminEmail()));
        updateSettings.setSiteDescription(normalizeNullable(request.siteDescription()));
        updateSettings.setBaseUrl(trimEndSlash(normalize(request.baseUrl())));
        updateSettings.setFooterText(normalizeNullable(request.footerText()));
        updateSettings.setThemeMode(normalizeThemeMode(request.themeMode()));
        updateSettings.setUpdatedAt(LocalDateTime.now());

        siteSettingsMapper.updateBySettingsKey(DEFAULT_SETTINGS_KEY, updateSettings);
    }

    /**
     * 初始化默认站点设置。
     */
    public void initializeDefaults() {
        // 只在首次启动或库中无记录时插入默认配置
        if (siteSettingsMapper.existsBySettingsKey(DEFAULT_SETTINGS_KEY)) {
            return;
        }

        SiteSettingsEntity settings = new SiteSettingsEntity();
        settings.setSettingsKey(DEFAULT_SETTINGS_KEY);
        settings.setSiteName(DEFAULT_SITE_NAME);
        settings.setAdminEmail(DEFAULT_ADMIN_EMAIL);
        settings.setSiteDescription(DEFAULT_SITE_DESCRIPTION);
        settings.setBaseUrl(DEFAULT_BASE_URL);
        settings.setFooterText(DEFAULT_FOOTER_TEXT);
        settings.setThemeMode(DEFAULT_THEME_MODE);
        siteSettingsMapper.insert(settings);
    }

    /**
     * 去掉首尾空格。
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 规范可为空的文本字段。
     */
    private String normalizeNullable(String value) {
        // 把空字符串统一转成 null，避免数据库里保留无意义空值
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 统一移除站点地址末尾斜杠。
     */
    private String trimEndSlash(String value) {
        if (value == null) {
            return null;
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 规范主题模式值，统一转成大写。
     */
    private String normalizeThemeMode(String value) {
        // 主题统一转大写，保证枚举值一致
        return normalize(value).toUpperCase();
    }
}
