package com.first.gateway.integration;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import com.first.gateway.service.auth.ConcurrencyLimiter;
import com.first.gateway.service.auth.MemoryApiKeyRateLimiter;
import com.first.gateway.service.auth.MemoryTpmRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class RateLimitIntegrationTest {

    @Autowired
    private MemoryApiKeyRateLimiter apiKeyRateLimiter;
    @Autowired
    private MemoryTpmRateLimiter tpmRateLimiter;
    @Autowired
    private ConcurrencyLimiter concurrencyLimiter;

    @Test
    void rpmLimit_memoryMode_blocks() {
        ApiKey apiKey = apiKey(101L, 3, -1, -1);

        assertDoesNotThrow(() -> apiKeyRateLimiter.check(apiKey));
        assertDoesNotThrow(() -> apiKeyRateLimiter.check(apiKey));
        assertDoesNotThrow(() -> apiKeyRateLimiter.check(apiKey));
        assertThrows(RateLimitExceededException.class, () -> apiKeyRateLimiter.check(apiKey));
    }

    @Test
    void tpmLimit_memoryMode_blocks() {
        ApiKey apiKey = apiKey(102L, -1, 1000, -1);

        assertThrows(RateLimitExceededException.class, () -> tpmRateLimiter.reserve(apiKey, 1500));
    }

    @Test
    void concurrency_memoryMode_blocks() {
        ApiKey apiKey = apiKey(103L, -1, -1, 1);

        assertDoesNotThrow(() -> concurrencyLimiter.acquire(apiKey));
        assertThrows(RateLimitExceededException.class, () -> concurrencyLimiter.acquire(apiKey));
    }

    private static ApiKey apiKey(long id, int rateLimit, int tpmLimit, int maxConcurrent) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setRateLimit(rateLimit);
        apiKey.setTpmLimit(tpmLimit);
        apiKey.setMaxConcurrent(maxConcurrent);
        return apiKey;
    }
}