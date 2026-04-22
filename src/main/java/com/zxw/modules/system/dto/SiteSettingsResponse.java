package com.zxw.modules.system.dto;

import java.time.LocalDateTime;

public record SiteSettingsResponse(
        Long id,
        String siteName,
        String adminEmail,
        String siteDescription,
        String baseUrl,
        String footerText,
        String themeMode,
        LocalDateTime updatedAt
) {
}
