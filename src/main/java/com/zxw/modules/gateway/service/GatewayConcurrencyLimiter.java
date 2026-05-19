package com.zxw.modules.gateway.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.config.GatewayConcurrencyProperties;
import com.zxw.modules.apikey.service.ApiKeyAuthService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GatewayConcurrencyLimiter {

    private final GatewayConcurrencyProperties properties;
    private final AtomicInteger globalRequests = new AtomicInteger();
    private final AtomicInteger globalStreams = new AtomicInteger();
    private final ConcurrentHashMap<Long, AtomicInteger> userRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> userStreams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> apiKeyRequests = new ConcurrentHashMap<>();

    public GatewayConcurrencyLimiter(GatewayConcurrencyProperties properties) {
        this.properties = properties;
    }

    public Permit acquireRequest(ApiKeyAuthService.AuthenticatedApiKey auth) {
        List<Runnable> releases = new ArrayList<>();
        try {
            acquireCounter(globalRequests, properties.maxGlobalRequestsLimit(), releases,
                    "Too many concurrent gateway requests");
            if (auth != null) {
                acquireMappedCounter(userRequests, auth.userId(), userRequestLimit(auth), releases,
                        "Too many concurrent requests for this user");
                acquireMappedCounter(apiKeyRequests, auth.id(), apiKeyRequestLimit(auth), releases,
                        "Too many concurrent requests for this API key");
            }
            return new Permit(releases);
        } catch (BusinessException ex) {
            releaseAll(releases);
            throw ex;
        }
    }

    public Permit acquireStream(ApiKeyAuthService.AuthenticatedApiKey auth) {
        List<Runnable> releases = new ArrayList<>();
        try {
            acquireCounter(globalStreams, properties.maxGlobalStreamsLimit(), releases,
                    "Too many concurrent streaming requests");
            if (auth != null) {
                acquireMappedCounter(userStreams, auth.userId(), userStreamLimit(auth), releases,
                        "Too many concurrent streaming requests for this user");
            }
            return new Permit(releases);
        } catch (BusinessException ex) {
            releaseAll(releases);
            throw ex;
        }
    }

    public long streamTimeoutMs() {
        return properties.streamTimeoutMs();
    }

    private int userRequestLimit(ApiKeyAuthService.AuthenticatedApiKey auth) {
        Integer customLimit = auth.maxConcurrentRequests();
        return customLimit != null && customLimit > 0 ? customLimit : properties.maxUserRequestsLimit();
    }

    private int apiKeyRequestLimit(ApiKeyAuthService.AuthenticatedApiKey auth) {
        Integer customLimit = auth.maxConcurrentRequests();
        return customLimit != null && customLimit > 0 ? customLimit : properties.maxApiKeyRequestsLimit();
    }

    private int userStreamLimit(ApiKeyAuthService.AuthenticatedApiKey auth) {
        Integer customLimit = auth.maxConcurrentStreams();
        return customLimit == null ? 0 : Math.max(0, customLimit);
    }

    private void acquireMappedCounter(ConcurrentHashMap<Long, AtomicInteger> counters,
                                      Long key,
                                      int limit,
                                      List<Runnable> releases,
                                      String message) {
        if (key == null || limit <= 0) {
            return;
        }
        AtomicInteger counter = counters.computeIfAbsent(key, ignored -> new AtomicInteger());
        acquireCounter(counter, limit, releases, message, () -> counters.remove(key, counter));
    }

    private void acquireCounter(AtomicInteger counter, int limit, List<Runnable> releases, String message) {
        acquireCounter(counter, limit, releases, message, null);
    }

    private void acquireCounter(AtomicInteger counter,
                                int limit,
                                List<Runnable> releases,
                                String message,
                                Runnable cleanup) {
        if (limit <= 0) {
            return;
        }
        int current = counter.incrementAndGet();
        releases.add(() -> {
            int remaining = counter.decrementAndGet();
            if (remaining <= 0 && cleanup != null) {
                cleanup.run();
            }
        });
        if (current > limit) {
            throw new BusinessException(429, message);
        }
    }

    private static void releaseAll(List<Runnable> releases) {
        for (int i = releases.size() - 1; i >= 0; i--) {
            releases.get(i).run();
        }
    }

    public static final class Permit implements AutoCloseable {
        private final List<Runnable> releases;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private Permit(List<Runnable> releases) {
            this.releases = releases;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                releaseAll(releases);
            }
        }
    }
}
