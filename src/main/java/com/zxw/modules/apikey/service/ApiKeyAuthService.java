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
public class ApiKeyAuthService {

    private final ApiKeyMapper apiKeyMapper;
    private final PasswordService passwordService;
    private final UserModelAccessService userModelAccessService;
    private final ApiKeyAuthCacheService apiKeyAuthCacheService;

    public ApiKeyAuthService(ApiKeyMapper apiKeyMapper,
                             PasswordService passwordService,
                             UserModelAccessService userModelAccessService,
                             ApiKeyAuthCacheService apiKeyAuthCacheService) {
        this.apiKeyMapper = apiKeyMapper;
        this.passwordService = passwordService;
        this.userModelAccessService = userModelAccessService;
        this.apiKeyAuthCacheService = apiKeyAuthCacheService;
    }

    public AuthenticatedApiKey authenticate(String bearerToken) {
        userModelAccessService.initializeDefaults();

        if (bearerToken == null || bearerToken.isBlank() || bearerToken.length() < AdminApiKeyService.ACCESS_KEY_PREFIX_LENGTH) {
            throw new BusinessException(401, "Invalid API key");
        }

        String accessKey = bearerToken.substring(0, AdminApiKeyService.ACCESS_KEY_PREFIX_LENGTH);
        List<AuthenticatedApiKey> items = apiKeyAuthCacheService.get(accessKey);
        if (items == null) {
            items = apiKeyMapper.selectAuthenticatedByAccessKey(accessKey).stream()
                    .map(item -> new AuthenticatedApiKey(
                            item.getId(),
                            item.getUserId(),
                            item.getUsername(),
                            item.getRoleCode(),
                            item.getSecretHash(),
                            item.getStatus(),
                            item.getUserPackageId(),
                            item.getPackageName(),
                            item.getPackageType(),
                            item.getModelGroupId(),
                            item.getGroupCode(),
                            item.getGroupName(),
                            item.getTotalQuota(),
                            item.getUsedQuota(),
                            item.getExpiresAt(),
                            item.getBalance(),
                            item.getPackageRestrictionEnabled() != null && item.getPackageRestrictionEnabled() == 1,
                            item.getMaxConcurrentRequests(),
                            item.getMaxConcurrentStreams()
                    ))
                    .toList();
            apiKeyAuthCacheService.put(accessKey, items);
        }

        for (AuthenticatedApiKey item : items) {
            if (passwordService.matches(bearerToken, item.secretHash())) {
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

    public void markUsed(Long apiKeyId) {
        if (!apiKeyAuthCacheService.shouldUpdateLastUsedAt(apiKeyId)) {
            return;
        }
        apiKeyMapper.updateLastUsedAt(apiKeyId, LocalDateTime.now());
    }

    public record AuthenticatedApiKey(
            Long id,
            Long userId,
            String username,
            String roleCode,
            String secretHash,
            String status,
            Long userPackageId,
            String userPackageName,
            String packageType,
            Long modelGroupId,
            String modelGroupCode,
            String modelGroupName,
            BigDecimal totalQuota,
            BigDecimal usedQuota,
            LocalDateTime expiresAt,
            BigDecimal balance,
            boolean packageRestrictionEnabled,
            Integer maxConcurrentRequests,
            Integer maxConcurrentStreams
    ) {
    }
}
