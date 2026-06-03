package com.first.gateway.service.auth;

import com.first.gateway.config.GatewayProperties;
import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConcurrencyLimiterTest {

    private MemoryConcurrencyLimiter memoryLimiter;

    @Mock
    private GatewayProperties gatewayProperties;
    @Mock
    private GatewayProperties.RateLimit rateLimit;
    @Mock
    private RedisConcurrencyLimiter redisLimiter;
    @Mock
    private ObjectProvider<RedisConcurrencyLimiter> redisProvider;

    @BeforeEach
    void setUp() {
        memoryLimiter = new MemoryConcurrencyLimiter();
        when(gatewayProperties.getRateLimit()).thenReturn(rateLimit);
    }

    @Test
    void acquire_underLimit_passes() {
        ApiKey apiKey = apiKey(1L, 3);

        assertNotNull(memoryLimiter.acquire(apiKey));
        assertNotNull(memoryLimiter.acquire(apiKey));
    }

    @Test
    void acquire_atLimit_rejects() {
        ApiKey apiKey = apiKey(2L, 3);
        memoryLimiter.acquire(apiKey);
        memoryLimiter.acquire(apiKey);
        memoryLimiter.acquire(apiKey);

        assertThrows(RateLimitExceededException.class, () -> memoryLimiter.acquire(apiKey));
    }

    @Test
    void release_decrementsConcurrency() {
        ApiKey apiKey = apiKey(3L, 3);
        String slot1 = memoryLimiter.acquire(apiKey);
        memoryLimiter.acquire(apiKey);
        memoryLimiter.acquire(apiKey);

        memoryLimiter.release(apiKey, slot1);

        assertDoesNotThrow(() -> memoryLimiter.acquire(apiKey));
    }

    @Test
    void acquire_negativeConcurrency_noLimit() {
        ApiKey apiKey = apiKey(4L, -1);

        assertDoesNotThrow(() -> memoryLimiter.acquire(apiKey));
        assertDoesNotThrow(() -> memoryLimiter.acquire(apiKey));
    }

    @Test
    void release_withoutAcquire_noError() {
        ApiKey apiKey = apiKey(5L, 3);

        assertDoesNotThrow(() -> memoryLimiter.release(apiKey, "missing-slot"));
    }

    @Test
    void redisFails_fallbackToMemory() {
        when(rateLimit.getType()).thenReturn("redis");
        when(redisProvider.getObject()).thenThrow(new RuntimeException("redis unavailable"));
        ApiKey apiKey = apiKey(6L, 5);

        ConcurrencyLimiter limiter = new ConcurrencyLimiter(gatewayProperties, memoryLimiter, redisProvider);

        assertDoesNotThrow(() -> limiter.acquire(apiKey));
    }

    @Test
    void acquireAndRelease_lifecycle() {
        when(rateLimit.getType()).thenReturn("memory");
        ApiKey apiKey = apiKey(7L, 2);
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(gatewayProperties, memoryLimiter, redisProvider);

        String slot = limiter.acquire(apiKey);
        assertNotNull(slot);
        assertDoesNotThrow(() -> limiter.release(apiKey, slot));
        assertDoesNotThrow(() -> limiter.acquire(apiKey));
    }

    private static ApiKey apiKey(long id, int maxConcurrent) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setMaxConcurrent(maxConcurrent);
        return apiKey;
    }
}