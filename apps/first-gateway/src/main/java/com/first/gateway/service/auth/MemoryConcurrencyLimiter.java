package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MemoryConcurrencyLimiter {

    private final ConcurrentHashMap<Long, AtomicInteger> active = new ConcurrentHashMap<>();

    public String acquire(ApiKey apiKey) {
        Integer limit = apiKey.getMaxConcurrent();
        if (limit == null || limit <= 0) {
            return UUID.randomUUID().toString();
        }
        AtomicInteger counter = active.computeIfAbsent(apiKey.getId(), id -> new AtomicInteger());
        int current = counter.incrementAndGet();
        if (current > limit) {
            counter.decrementAndGet();
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
        AtomicInteger counter = active.get(apiKey.getId());
        if (counter != null) {
            counter.updateAndGet(value -> Math.max(0, value - 1));
        }
    }
}
