package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "gateway.rate-limit.type", havingValue = "redis")
public class RedisConcurrencyLimiter {

    private static final String KEY_PREFIX = "rate:concurrent:";
    private static final Duration TTL = Duration.ofSeconds(120);

    private final StringRedisTemplate redisTemplate;

    public RedisConcurrencyLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String acquire(ApiKey apiKey) {
        Integer limit = apiKey.getMaxConcurrent();
        if (limit == null || limit <= 0) {
            return UUID.randomUUID().toString();
        }
        String key = KEY_PREFIX + apiKey.getId();
        Long current = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, TTL);
        if (current != null && current > limit) {
            redisTemplate.opsForValue().decrement(key);
            throw new RateLimitExceededException(
                RateLimitExceededException.LimitType.CONCURRENT,
                30,
                limit,
                0);
        }
        return UUID.randomUUID().toString();
    }

    public void release(ApiKey apiKey, String slotId) {
        if (apiKey == null || slotId == null) {
            return;
        }
        Integer limit = apiKey.getMaxConcurrent();
        if (limit == null || limit <= 0) {
            return;
        }
        String key = KEY_PREFIX + apiKey.getId();
        Long current = redisTemplate.opsForValue().decrement(key);
        if (current != null && current < 0) {
            redisTemplate.opsForValue().set(key, "0");
        }
    }
}
