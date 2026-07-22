package com.zxw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.upstream")
public record GatewayUpstreamProperties(
        Integer connectTimeoutMs,
        Integer readTimeoutMs,
        Integer writeTimeoutMs,
        Integer callTimeoutMs,
        Integer streamReadTimeoutMs,
        Integer maxRequests,
        Integer maxRequestsPerHost,
        Integer maxIdleConnections,
        Integer keepAliveSeconds,
        Boolean retryOnConnectionFailure
) {

    public int connectTimeoutMsValue() {
        return positiveOrDefault(connectTimeoutMs, 20_000);
    }

    public int readTimeoutMsValue() {
        return positiveOrDefault(readTimeoutMs, 60_000);
    }

    public int writeTimeoutMsValue() {
        return positiveOrDefault(writeTimeoutMs, 60_000);
    }

    public int callTimeoutMsValue() {
        return positiveOrDefault(callTimeoutMs, 60_000);
    }

    public int streamReadTimeoutMsValue() {
        return positiveOrDefault(streamReadTimeoutMs, 300_000);
    }

    public int maxRequestsValue() {
        return positiveOrDefault(maxRequests, 256);
    }

    public int maxRequestsPerHostValue() {
        return positiveOrDefault(maxRequestsPerHost, 64);
    }

    public int maxIdleConnectionsValue() {
        return positiveOrDefault(maxIdleConnections, 64);
    }

    public int keepAliveSecondsValue() {
        return positiveOrDefault(keepAliveSeconds, 300);
    }

    public boolean retryOnConnectionFailureEnabled() {
        return retryOnConnectionFailure == null || retryOnConnectionFailure;
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}
