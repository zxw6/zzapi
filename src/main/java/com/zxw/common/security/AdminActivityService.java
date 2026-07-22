package com.zxw.common.security;

import com.zxw.persistence.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AdminActivityService {

    private static final Logger log = LoggerFactory.getLogger(AdminActivityService.class);
    private static final Duration TOUCH_INTERVAL = Duration.ofSeconds(60);
    private static final String ACTIVE_TOUCH_PREFIX = "auth:admin:last-active:";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;

    public AdminActivityService(StringRedisTemplate stringRedisTemplate, UserMapper userMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userMapper = userMapper;
    }

    public void markActive(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(ACTIVE_TOUCH_PREFIX + userId, "1", TOUCH_INTERVAL);
            if (!Boolean.TRUE.equals(acquired)) {
                return;
            }
        } catch (Exception ex) {
            log.warn("Failed to throttle admin active update: {}", ex.getMessage());
        }
        userMapper.updateLastActiveAt(userId, LocalDateTime.now());
    }
}
