package com.zxw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.concurrency")
public record GatewayConcurrencyProperties(
        Integer maxGlobalRequests,
        Integer maxGlobalStreams,
        Integer maxUserRequests,
        Integer maxApiKeyRequests,
        Integer maxSessionRequests,
        Stream stream
) {

    public GatewayConcurrencyProperties {
        stream = stream == null ? new Stream(null, null, null, null) : stream;
    }

    public int maxGlobalRequestsLimit() {
        return limit(maxGlobalRequests);
    }

    public int maxGlobalStreamsLimit() {
        return limit(maxGlobalStreams);
    }

    public int maxUserRequestsLimit() {
        return limit(maxUserRequests);
    }

    public int maxApiKeyRequestsLimit() {
        return limit(maxApiKeyRequests);
    }

    public int maxSessionRequestsLimit() {
        return limit(maxSessionRequests);
    }

    public int streamCoreThreads() {
        return positiveOrDefault(stream.coreThreads(), 16);
    }

    public int streamMaxThreads() {
        return Math.max(streamCoreThreads(), positiveOrDefault(stream.maxThreads(), 64));
    }

    public int streamQueueCapacity() {
        return Math.max(0, positiveOrDefault(stream.queueCapacity(), 200));
    }

    public long streamTimeoutMs() {
        return positiveOrDefault(stream.timeoutMs(), 300_000);
    }

    private static int limit(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    public record Stream(
            Integer coreThreads,
            Integer maxThreads,
            Integer queueCapacity,
            Integer timeoutMs
    ) {
    }
}
