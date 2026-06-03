package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "gateway.rate-limit.type", havingValue = "redis")
public class RedisTpmRateLimiter {

    private static final String BUCKET_KEY = "rate:tpm:%d";

    private final StringRedisTemplate redisTemplate;

    public RedisTpmRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long reserve(ApiKey apiKey, long tokensNeeded) {
        Integer limit = apiKey.getTpmLimit();
        if (limit == null || limit <= 0 || tokensNeeded <= 0) {
            return 0;
        }
        String key = BUCKET_KEY.formatted(apiKey.getId());
        double available = readAvailable(key, limit);
        if (available < tokensNeeded) {
            throw new RateLimitExceededException(
                RateLimitExceededException.LimitType.TPM,
                60,
                limit,
                (int) Math.max(0, available));
        }
        writeAvailable(key, limit, available - tokensNeeded);
        return tokensNeeded;
    }

    public void refund(ApiKey apiKey, long reserved, long actual) {
        if (reserved <= 0) {
            return;
        }
        Integer limit = apiKey.getTpmLimit();
        if (limit == null || limit <= 0) {
            return;
        }
        long refund = Math.max(0, reserved - Math.max(0, actual));
        if (refund <= 0) {
            return;
        }
        String key = BUCKET_KEY.formatted(apiKey.getId());
        double available = readAvailable(key, limit);
        writeAvailable(key, limit, Math.min(limit, available + refund));
    }

    private double readAvailable(String key, int limit) {
        String value = redisTemplate.opsForValue().get(key);
        long lastRefill = System.currentTimeMillis();
        double tokens = limit;
        if (value != null) {
            String[] parts = value.split(":");
            tokens = Double.parseDouble(parts[0]);
            lastRefill = Long.parseLong(parts[1]);
        }
        long now = System.currentTimeMillis();
        double ratePerMs = limit / 60_000.0;
        tokens = Math.min(limit, tokens + Math.max(0, now - lastRefill) * ratePerMs);
        writeAvailable(key, limit, tokens);
        return tokens;
    }

    private void writeAvailable(String key, int limit, double tokens) {
        redisTemplate.opsForValue().set(key, tokens + ":" + System.currentTimeMillis());
        redisTemplate.expire(key, java.time.Duration.ofSeconds(120));
    }
}
