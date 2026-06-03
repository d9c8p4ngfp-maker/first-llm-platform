package com.first.gateway.service.auth;

import com.first.gateway.config.GatewayProperties;
import com.first.gateway.domain.entity.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyRateLimiter.class);

    private final GatewayProperties gatewayProperties;
    private final MemoryApiKeyRateLimiter memoryApiKeyRateLimiter;
    private final ObjectProvider<RedisApiKeyRateLimiter> redisApiKeyRateLimiter;

    public ApiKeyRateLimiter(GatewayProperties gatewayProperties,
                             MemoryApiKeyRateLimiter memoryApiKeyRateLimiter,
                             ObjectProvider<RedisApiKeyRateLimiter> redisApiKeyRateLimiter) {
        this.gatewayProperties = gatewayProperties;
        this.memoryApiKeyRateLimiter = memoryApiKeyRateLimiter;
        this.redisApiKeyRateLimiter = redisApiKeyRateLimiter;
    }

    public void check(ApiKey apiKey) {
        if (useRedis()) {
            try {
                redisApiKeyRateLimiter.getObject().check(apiKey);
                return;
            } catch (Exception ex) {
                log.warn("redis rate limiter failed, fallback to memory: {}", ex.getMessage());
            }
        }
        memoryApiKeyRateLimiter.check(apiKey);
    }

    private boolean useRedis() {
        return "redis".equalsIgnoreCase(gatewayProperties.getRateLimit().getType());
    }
}
