package com.first.gateway.service.auth;

import com.first.gateway.config.GatewayProperties;
import com.first.gateway.domain.entity.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ConcurrencyLimiter {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyLimiter.class);

    private final GatewayProperties gatewayProperties;
    private final MemoryConcurrencyLimiter memoryConcurrencyLimiter;
    private final ObjectProvider<RedisConcurrencyLimiter> redisConcurrencyLimiter;

    public ConcurrencyLimiter(GatewayProperties gatewayProperties,
                              MemoryConcurrencyLimiter memoryConcurrencyLimiter,
                              ObjectProvider<RedisConcurrencyLimiter> redisConcurrencyLimiter) {
        this.gatewayProperties = gatewayProperties;
        this.memoryConcurrencyLimiter = memoryConcurrencyLimiter;
        this.redisConcurrencyLimiter = redisConcurrencyLimiter;
    }

    public String acquire(ApiKey apiKey) {
        if (useRedis()) {
            try {
                return redisConcurrencyLimiter.getObject().acquire(apiKey);
            } catch (RuntimeException ex) {
                if (!(ex instanceof com.first.gateway.infra.error.RateLimitExceededException)) {
                    log.warn("redis concurrency limiter failed, fallback to memory: {}", ex.getMessage());
                    return memoryConcurrencyLimiter.acquire(apiKey);
                }
                throw ex;
            }
        }
        return memoryConcurrencyLimiter.acquire(apiKey);
    }

    public void release(ApiKey apiKey, String slotId) {
        if (useRedis()) {
            try {
                redisConcurrencyLimiter.getObject().release(apiKey, slotId);
                return;
            } catch (Exception ex) {
                log.warn("redis concurrency release failed, fallback to memory: {}", ex.getMessage());
            }
        }
        memoryConcurrencyLimiter.release(apiKey, slotId);
    }

    private boolean useRedis() {
        return "redis".equalsIgnoreCase(gatewayProperties.getRateLimit().getType());
    }
}
