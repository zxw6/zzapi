package com.zxw.config;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
public class GatewayConcurrencyConfig {

    @Bean(name = "gatewayStreamTaskExecutor")
    public ThreadPoolTaskExecutor gatewayStreamTaskExecutor(GatewayConcurrencyProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.streamCoreThreads());
        executor.setMaxPoolSize(properties.streamMaxThreads());
        executor.setQueueCapacity(properties.streamQueueCapacity());
        executor.setThreadNamePrefix("gateway-upstream-stream-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "gatewayLogTaskExecutor")
    public ThreadPoolTaskExecutor gatewayLogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("gateway-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public OkHttpClient gatewayOkHttpClient(GatewayUpstreamProperties properties) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(properties.maxRequestsValue());
        dispatcher.setMaxRequestsPerHost(properties.maxRequestsPerHostValue());
        ConnectionPool connectionPool = new ConnectionPool(
                properties.maxIdleConnectionsValue(),
                properties.keepAliveSecondsValue(),
                TimeUnit.SECONDS
        );
        return new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMsValue()))
                .readTimeout(Duration.ofMillis(properties.readTimeoutMsValue()))
                .writeTimeout(Duration.ofMillis(properties.writeTimeoutMsValue()))
                .callTimeout(Duration.ofMillis(properties.callTimeoutMsValue()))
                .retryOnConnectionFailure(properties.retryOnConnectionFailureEnabled())
                .build();
    }

    @Bean
    public HttpClient gatewayJavaHttpClient(GatewayUpstreamProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMsValue()))
                .build();
    }
}
