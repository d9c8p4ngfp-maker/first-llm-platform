package com.first.gateway.service.auth;

import com.first.gateway.config.GatewayProperties;
import com.first.gateway.domain.entity.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class TpmRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TpmRateLimiter.class);

    private final GatewayProperties gatewayProperties;
    private final MemoryTpmRateLimiter memoryTpmRateLimiter;
    private final ObjectProvider<RedisTpmRateLimiter> redisTpmRateLimiter;

    public TpmRateLimiter(GatewayProperties gatewayProperties,
                          MemoryTpmRateLimiter memoryTpmRateLimiter,
                          ObjectProvider<RedisTpmRateLimiter> redisTpmRateLimiter) {
        this.gatewayProperties = gatewayProperties;
        this.memoryTpmRateLimiter = memoryTpmRateLimiter;
        this.redisTpmRateLimiter = redisTpmRateLimiter;
    }

    public long reserve(ApiKey apiKey, long tokensNeeded) {
        if (useRedis()) {
            try {
                return redisTpmRateLimiter.getObject().reserve(apiKey, tokensNeeded);
            } catch (Exception ex) {
                log.warn("redis tpm limiter failed, fallback to memory: {}", ex.getMessage());
            }
        }
        return memoryTpmRateLimiter.reserve(apiKey, tokensNeeded);
    }

    public void refund(ApiKey apiKey, long reserved, long actual) {
        if (useRedis()) {
            try {
                redisTpmRateLimiter.getObject().refund(apiKey, reserved, actual);
                return;
            } catch (Exception ex) {
                log.warn("redis tpm refund failed, fallback to memory: {}", ex.getMessage());
            }
        }
        memoryTpmRateLimiter.refund(apiKey, reserved, actual);
    }

    private boolean useRedis() {
        return "redis".equalsIgnoreCase(gatewayProperties.getRateLimit().getType());
    }
}
