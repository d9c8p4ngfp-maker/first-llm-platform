package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MemoryApiKeyRateLimiter {

    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<Long, WindowCounter> counters = new ConcurrentHashMap<>();

    public void check(ApiKey apiKey) {
        Integer limit = apiKey.getRateLimit();
        if (limit == null || limit <= 0) {
            return;
        }
        long windowStart = currentWindowStart();
        WindowCounter counter = counters.compute(apiKey.getId(),
            (id, existing) -> existing == null || existing.windowStart != windowStart
                ? new WindowCounter(windowStart)
                : existing);
        if (counter.count.incrementAndGet() > limit) {
            throw new RateLimitExceededException(
                RateLimitExceededException.LimitType.RPM,
                60,
                limit,
                0);
        }
    }

    private static long currentWindowStart() {
        long epochSecond = Instant.now().getEpochSecond();
        return epochSecond - (epochSecond % WINDOW_SECONDS);
    }

    private static final class WindowCounter {
        private final long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        private WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
