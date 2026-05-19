package com.zxw.modules.apikey.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.zxw.persistence.entity.ApiKeyEntity;
import com.zxw.persistence.mapper.ApiKeyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApiKeyAuthCacheService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthCacheService.class);
    private static final Duration AUTH_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration TOUCH_INTERVAL = Duration.ofSeconds(60);
    private static final String API_KEY_AUTH_PREFIX = "auth:apikey:v1:";
    private static final String API_KEY_TOUCH_PREFIX = "auth:apikey:last-used:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final CollectionType authListType;

    public ApiKeyAuthCacheService(StringRedisTemplate stringRedisTemplate,
                                  ObjectMapper objectMapper,
                                  ApiKeyMapper apiKeyMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.apiKeyMapper = apiKeyMapper;
        this.authListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, ApiKeyAuthService.AuthenticatedApiKey.class);
    }

    public List<ApiKeyAuthService.AuthenticatedApiKey> get(String accessKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(authKey(accessKey));
            if (cached == null || cached.isBlank()) {
                return null;
            }
            return objectMapper.readValue(cached, authListType);
        } catch (Exception ex) {
            log.warn("Failed to read API key auth cache: {}", ex.getMessage());
            return null;
        }
    }

    public void put(String accessKey, List<ApiKeyAuthService.AuthenticatedApiKey> items) {
        if (accessKey == null || accessKey.isBlank() || items == null || items.isEmpty()) {
            return;
        }
        Duration ttl = resolveTtl(items);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(authKey(accessKey), objectMapper.writeValueAsString(items), ttl);
        } catch (Exception ex) {
            log.warn("Failed to write API key auth cache: {}", ex.getMessage());
        }
    }

    public void evictByApiKeyId(Long apiKeyId) {
        if (apiKeyId == null) {
            return;
        }
        try {
            ApiKeyEntity entity = apiKeyMapper.selectById(apiKeyId);
            if (entity != null) {
                evictByAccessKey(entity.getAccessKey());
            }
        } catch (Exception ex) {
            log.warn("Failed to evict API key auth cache by id={}: {}", apiKeyId, ex.getMessage());
        }
    }

    public void evictByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            List<ApiKeyEntity> keys = apiKeyMapper.selectByUserId(userId);
            for (ApiKeyEntity key : keys) {
                evictByAccessKey(key.getAccessKey());
            }
        } catch (Exception ex) {
            log.warn("Failed to evict API key auth cache by userId={}: {}", userId, ex.getMessage());
        }
    }

    public boolean shouldUpdateLastUsedAt(Long apiKeyId) {
        if (apiKeyId == null) {
            return false;
        }
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(API_KEY_TOUCH_PREFIX + apiKeyId, "1", TOUCH_INTERVAL);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception ex) {
            log.warn("Failed to throttle API key last-used update: {}", ex.getMessage());
            return true;
        }
    }

    private void evictByAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            return;
        }
        stringRedisTemplate.delete(authKey(accessKey));
    }

    private Duration resolveTtl(List<ApiKeyAuthService.AuthenticatedApiKey> items) {
        LocalDateTime now = LocalDateTime.now();
        Duration ttl = AUTH_CACHE_TTL;
        for (ApiKeyAuthService.AuthenticatedApiKey item : items) {
            if (item.expiresAt() != null) {
                Duration untilExpire = Duration.between(now, item.expiresAt());
                if (untilExpire.compareTo(ttl) < 0) {
                    ttl = untilExpire;
                }
            }
        }
        return ttl;
    }

    private String authKey(String accessKey) {
        return API_KEY_AUTH_PREFIX + accessKey;
    }
}
