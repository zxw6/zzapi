package com.zxw.modules.request.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RequestLogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(RequestLogCleanupTask.class);

    private static final int RETENTION_DAYS = 3;

    private final JdbcTemplate jdbcTemplate;

    public RequestLogCleanupTask(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 17 3 */3 * ?")
    public void cleanup() {
        long start = System.currentTimeMillis();
        Integer deleted = jdbcTemplate.update(
                "delete from request_logs where created_at < date_sub(now(), interval ? day)",
                RETENTION_DAYS);
        log.info("[request-logs-cleanup] deleted {} rows older than {} days, cost {} ms",
                deleted, RETENTION_DAYS, System.currentTimeMillis() - start);
    }
}
