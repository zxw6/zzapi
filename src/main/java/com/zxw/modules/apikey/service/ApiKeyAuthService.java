package com.zxw.modules.apikey.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.access.service.UserModelAccessService;
import com.zxw.persistence.mapper.ApiKeyMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * API Key 鉴权服务。
 * 负责根据 Bearer Token 识别调用方身份，并更新最后使用时间。
 */
public class ApiKeyAuthService {

    private final ApiKeyMapper apiKeyMapper;
    private final PasswordService passwordService;
    private final UserModelAccessService userModelAccessService;

    public ApiKeyAuthService(ApiKeyMapper apiKeyMapper,
                             PasswordService passwordService,
                             UserModelAccessService userModelAccessService) {
        this.apiKeyMapper = apiKeyMapper;
        this.passwordService = passwordService;
        this.userModelAccessService = userModelAccessService;
    }

    /**
     * 校验 Bearer Token 并解析成已认证的 API Key 信息。
     */
    public AuthenticatedApiKey authenticate(String bearerToken) {
        // 保证套餐限制相关默认数据已初始化
        userModelAccessService.initializeDefaults();

        // 先做最基础的 Bearer Token 格式校验
        if (bearerToken == null || bearerToken.isBlank() || bearerToken.length() < AdminApiKeyService.ACCESS_KEY_PREFIX_LENGTH) {
            throw new BusinessException(401, "Invalid API key");
        }

        // 通过访问前缀快速缩小候选集合，再逐个比对哈希
        String accessKey = bearerToken.substring(0, AdminApiKeyService.ACCESS_KEY_PREFIX_LENGTH);
        List<AuthenticatedApiKey> items = apiKeyMapper.selectAuthenticatedByAccessKey(accessKey).stream()
                .map(item -> new AuthenticatedApiKey(
                        item.getId(),
                        item.getUserId(),
                        item.getUsername(),
                        item.getRoleCode(),
                        item.getSecretHash(),
                        item.getStatus(),
                        item.getUserPackageId(),
                        item.getPackageName(),
                        item.getModelGroupId(),
                        item.getGroupCode(),
                        item.getGroupName(),
                        item.getTotalQuota(),
                        item.getUsedQuota(),
                        item.getExpiresAt(),
                        item.getBalance(),
                        item.getPackageRestrictionEnabled() != null && item.getPackageRestrictionEnabled() == 1
                ))
                .toList();

        for (AuthenticatedApiKey item : items) {
            if (passwordService.matches(bearerToken, item.secretHash())) {
                // 命中后继续校验状态与过期时间
                if (!"ACTIVE".equals(item.status())) {
                    throw new BusinessException(403, "API key is disabled");
                }
                if (item.expiresAt() != null && item.expiresAt().isBefore(LocalDateTime.now())) {
                    throw new BusinessException(403, "API key has expired");
                }
                return item;
            }
        }
        throw new BusinessException(401, "Invalid API key");
    }

    /**
     * 更新 API Key 最近使用时间。
     */
    public void markUsed(Long apiKeyId) {
        // 更新 API Key 最近一次使用时间
        apiKeyMapper.updateLastUsedAt(apiKeyId, LocalDateTime.now());
    }

    /**
     * 已认证 API Key 视图对象。
     */
    public record AuthenticatedApiKey(
            Long id,
            Long userId,
            String username,
            String roleCode,
            String secretHash,
            String status,
            Long userPackageId,
            String userPackageName,
            Long modelGroupId,
            String modelGroupCode,
            String modelGroupName,
            BigDecimal totalQuota,
            BigDecimal usedQuota,
            LocalDateTime expiresAt,
            BigDecimal balance,
            boolean packageRestrictionEnabled
    ) {
    }
}
