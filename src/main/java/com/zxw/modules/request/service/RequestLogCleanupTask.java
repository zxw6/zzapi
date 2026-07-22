package com.zxw.modules.request.service;

import com.zxw.persistence.mapper.RequestLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnBean(RequestLogMapper.class)
/**
 * 请求日志清理任务。
 * 按固定周期清理历史日志，避免请求表无限增长。
 */
public class RequestLogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(RequestLogCleanupTask.class);

    // 请求日志仅保留最近几天，避免表体积持续膨胀
    private static final int RETENTION_DAYS = 3;

    private final RequestLogMapper requestLogMapper;

    public RequestLogCleanupTask(RequestLogMapper requestLogMapper) {
        this.requestLogMapper = requestLogMapper;
    }

    /**
     * 定时删除超过保留天数的请求日志。
     */
    @Scheduled(cron = "0 17 3 */3 * ?")
    public void cleanup() {
        long start = System.currentTimeMillis();
        int deleted = requestLogMapper.deleteOlderThan(LocalDateTime.now().minusDays(RETENTION_DAYS));
        log.info("[request-logs-cleanup] deleted {} rows older than {} days, cost {} ms",
                deleted, RETENTION_DAYS, System.currentTimeMillis() - start);
    }
}
