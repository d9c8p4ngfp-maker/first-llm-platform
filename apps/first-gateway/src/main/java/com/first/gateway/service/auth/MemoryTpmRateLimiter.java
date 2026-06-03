package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryTpmRateLimiter {

    private final ConcurrentHashMap<Long, TokenBucket> buckets = new ConcurrentHashMap<>();

    public long reserve(ApiKey apiKey, long tokensNeeded) {
        Integer limit = apiKey.getTpmLimit();
        if (limit == null || limit <= 0 || tokensNeeded <= 0) {
            return 0;
        }
        TokenBucket bucket = buckets.computeIfAbsent(apiKey.getId(), id -> new TokenBucket(limit));
        bucket.syncLimit(limit);
        if (!bucket.tryConsume(tokensNeeded)) {
            throw new RateLimitExceededException(
                RateLimitExceededException.LimitType.TPM,
                60,
                limit,
                (int) Math.max(0, bucket.availableTokens()));
        }
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
        TokenBucket bucket = buckets.computeIfAbsent(apiKey.getId(), id -> new TokenBucket(limit));
        bucket.syncLimit(limit);
        bucket.refund(refund);
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillMs;
        private int capacity;

        private TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillMs = System.currentTimeMillis();
        }

        private synchronized void syncLimit(int limit) {
            this.capacity = limit;
            refill();
            this.tokens = Math.min(this.tokens, capacity);
        }

        private synchronized boolean tryConsume(long needed) {
            refill();
            if (tokens < needed) {
                return false;
            }
            tokens -= needed;
            return true;
        }

        private synchronized void refund(long amount) {
            refill();
            tokens = Math.min(capacity, tokens + amount);
        }

        private synchronized double availableTokens() {
            refill();
            return tokens;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now <= lastRefillMs) {
                return;
            }
            double ratePerMs = capacity / 60_000.0;
            tokens = Math.min(capacity, tokens + (now - lastRefillMs) * ratePerMs);
            lastRefillMs = now;
        }
    }
}
