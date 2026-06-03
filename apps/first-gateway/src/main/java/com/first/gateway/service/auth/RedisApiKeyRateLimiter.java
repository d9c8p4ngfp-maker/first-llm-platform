package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "gateway.rate-limit.type", havingValue = "redis")
public class RedisApiKeyRateLimiter {

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>("""
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
        local count = redis.call('ZCARD', key)
        if count >= limit then return 0 end
        redis.call('ZADD', key, now, now .. '-' .. math.random())
        redis.call('EXPIRE', key, window)
        return 1
        """, Long.class);

    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public RedisApiKeyRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void check(ApiKey apiKey) {
        Integer limit = apiKey.getRateLimit();
        if (limit == null || limit <= 0) {
            return;
        }
        String key = "rate:apikey:" + apiKey.getId();
        long now = Instant.now().getEpochSecond();
        Long allowed = redisTemplate.execute(
            SLIDING_WINDOW_SCRIPT,
            List.of(key),
            String.valueOf(limit),
            String.valueOf(WINDOW_SECONDS),
            String.valueOf(now));
        if (allowed == null || allowed == 0L) {
            throw new RateLimitExceededException(
                RateLimitExceededException.LimitType.RPM,
                60,
                limit,
                0);
        }
    }
}
