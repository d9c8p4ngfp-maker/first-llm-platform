package com.first.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private Retry retry = new Retry();
    private Timeout timeout = new Timeout();
    private Billing billing = new Billing();
    private Crypto crypto = new Crypto();
    private Trace trace = new Trace();
    private RateLimit rateLimit = new RateLimit();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Getter
    @Setter
    public static class RateLimit {
        private String type = "memory";
    }

    @Getter
    @Setter
    public static class CircuitBreaker {
        private int failureThreshold = 5;
        private int openDurationSeconds = 30;
    }

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
    }

    @Getter
    @Setter
    public static class Timeout {
        private int connectMs = 5000;
        private int readMs = 60000;
        private int streamFirstTokenMs = 30000;
        private int streamTotalMs = 300000;
        private int sseHeartbeatIntervalMs = 15000;
    }

    @Getter
    @Setter
    public static class Billing {
        private double defaultRatio = 1.0;
        private String noUsageStrategy = "estimate";
    }

    @Getter
    @Setter
    public static class Crypto {
        private String channelKeySecret;
    }

    @Getter
    @Setter
    public static class Trace {
        private String headerName = "X-Request-Id";
    }
}