package com.first.gateway.service.auth.admin;

import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class JwtBlacklistStore {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistStore.class);
    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    JwtBlacklistStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    void push(String jti, long ttlMs) {
        try {
            redis.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofMillis(ttlMs));
        } catch (Exception e) {
            log.error("Failed to push JWT blacklist entry {}: {}", jti, e.getMessage());
            throw new GatewayException(GatewayError.SERVICE_UNAVAILABLE, "JWT blacklist unavailable");
        }
    }

    boolean contains(String jti) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
        } catch (Exception e) {
            log.error("Failed to check JWT blacklist for {}: {}", jti, e.getMessage());
            throw new GatewayException(GatewayError.SERVICE_UNAVAILABLE, "JWT blacklist unavailable");
        }
    }
}
