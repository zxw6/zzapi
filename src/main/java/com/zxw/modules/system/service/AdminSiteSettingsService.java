package com.zxw.modules.system.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.modules.system.dto.SiteSettingsResponse;
import com.zxw.modules.system.dto.SiteSettingsUpdateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Service
public class AdminSiteSettingsService {

    private static final String DEFAULT_SETTINGS_KEY = "DEFAULT";
    private static final String DEFAULT_SITE_NAME = "API Hub 中转站";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@apihub.io";
    private static final String DEFAULT_SITE_DESCRIPTION = "企业级 AI API 中转管理平台";
    private static final String DEFAULT_BASE_URL = "https://api.yourdomain.com";
    private static final String DEFAULT_FOOTER_TEXT = "Powered by API Hub";
    private static final String DEFAULT_THEME_MODE = "LIGHT";

    private final JdbcTemplate jdbcTemplate;

    public AdminSiteSettingsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SiteSettingsResponse getSiteSettings() {
        AdminContext.requireAdmin();
        initializeDefaults();

        return jdbcTemplate.query(
                """
                select id, site_name, admin_email, site_description, base_url, footer_text, theme_mode, updated_at
                from site_settings
                where settings_key = ?
                limit 1
                """,
                rs -> {
                    if (!rs.next()) {
                        throw new BusinessException("站点信息不存在");
                    }
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    return new SiteSettingsResponse(
                            rs.getLong("id"),
                            rs.getString("site_name"),
                            rs.getString("admin_email"),
                            rs.getString("site_description"),
                            rs.getString("base_url"),
                            rs.getString("footer_text"),
                            rs.getString("theme_mode"),
                            updatedAt == null ? null : updatedAt.toLocalDateTime()
                    );
                },
                DEFAULT_SETTINGS_KEY
        );
    }

    @Transactional
    public void updateSiteSettings(SiteSettingsUpdateRequest request) {
        AdminContext.requireAdmin();
        initializeDefaults();

        jdbcTemplate.update(
                """
                update site_settings
                set site_name = ?,
                    admin_email = ?,
                    site_description = ?,
                    base_url = ?,
                    footer_text = ?,
                    theme_mode = ?,
                    updated_at = now()
                where settings_key = ?
                """,
                normalize(request.siteName()),
                normalize(request.adminEmail()),
                normalizeNullable(request.siteDescription()),
                trimEndSlash(normalize(request.baseUrl())),
                normalizeNullable(request.footerText()),
                normalizeThemeMode(request.themeMode()),
                DEFAULT_SETTINGS_KEY
        );
    }

    public void initializeDefaults() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from site_settings where settings_key = ?",
                Integer.class,
                DEFAULT_SETTINGS_KEY
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                insert into site_settings (settings_key, site_name, admin_email, site_description, base_url, footer_text, theme_mode)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                DEFAULT_SETTINGS_KEY,
                DEFAULT_SITE_NAME,
                DEFAULT_ADMIN_EMAIL,
                DEFAULT_SITE_DESCRIPTION,
                DEFAULT_BASE_URL,
                DEFAULT_FOOTER_TEXT,
                DEFAULT_THEME_MODE
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimEndSlash(String value) {
        if (value == null) {
            return null;
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String normalizeThemeMode(String value) {
        return normalize(value).toUpperCase();
    }
}
