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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmRateLimiterTest {

    private MemoryTpmRateLimiter memoryLimiter;

    @Mock
    private GatewayProperties gatewayProperties;
    @Mock
    private GatewayProperties.RateLimit rateLimit;
    @Mock
    private RedisTpmRateLimiter redisLimiter;
    @Mock
    private ObjectProvider<RedisTpmRateLimiter> redisProvider;

    @BeforeEach
    void setUp() {
        memoryLimiter = new MemoryTpmRateLimiter();
        when(gatewayProperties.getRateLimit()).thenReturn(rateLimit);
    }

    @Test
    void check_underLimit_passes() {
        ApiKey apiKey = apiKey(1L, 100_000);

        assertEquals(50_000, memoryLimiter.reserve(apiKey, 50_000));
    }

    @Test
    void check_atLimit_rejects() {
        ApiKey apiKey = apiKey(1L, 10);
        memoryLimiter.reserve(apiKey, 10);

        assertThrows(RateLimitExceededException.class, () -> memoryLimiter.reserve(apiKey, 1));
    }

    @Test
    void check_refillOverTime_passes() throws InterruptedException {
        ApiKey apiKey = apiKey(1L, 600);
        memoryLimiter.reserve(apiKey, 600);

        assertThrows(RateLimitExceededException.class, () -> memoryLimiter.reserve(apiKey, 1));

        Thread.sleep(150);

        assertDoesNotThrow(() -> memoryLimiter.reserve(apiKey, 1));
    }

    @Test
    void check_negativeTpmLimit_noLimit() {
        ApiKey apiKey = apiKey(1L, -1);

        assertEquals(0, memoryLimiter.reserve(apiKey, 999_999));
    }

    @Test
    void refund_restoresBucket() {
        ApiKey apiKey = apiKey(1L, 10);
        memoryLimiter.reserve(apiKey, 10);
        memoryLimiter.refund(apiKey, 10, 3);

        assertDoesNotThrow(() -> memoryLimiter.reserve(apiKey, 7));
        assertThrows(RateLimitExceededException.class, () -> memoryLimiter.reserve(apiKey, 1));
    }

    @Test
    void useRedis_callsRedisImpl() {
        when(rateLimit.getType()).thenReturn("redis");
        when(redisProvider.getObject()).thenReturn(redisLimiter);
        ApiKey apiKey = apiKey(2L, 100_000);
        when(redisLimiter.reserve(apiKey, 500)).thenReturn(500L);

        TpmRateLimiter limiter = new TpmRateLimiter(gatewayProperties, memoryLimiter, redisProvider);
        long reserved = limiter.reserve(apiKey, 500);

        assertEquals(500, reserved);
        verify(redisLimiter).reserve(apiKey, 500);
    }

    @Test
    void useMemory_callsMemoryImpl() {
        when(rateLimit.getType()).thenReturn("memory");
        ApiKey apiKey = apiKey(3L, 100_000);

        TpmRateLimiter limiter = new TpmRateLimiter(gatewayProperties, memoryLimiter, redisProvider);
        long reserved = limiter.reserve(apiKey, 1_000);

        assertEquals(1_000, reserved);
    }

    @Test
    void redisFails_fallbackToMemory() {
        when(rateLimit.getType()).thenReturn("redis");
        when(redisProvider.getObject()).thenThrow(new RuntimeException("redis unavailable"));
        ApiKey apiKey = apiKey(4L, 100_000);

        TpmRateLimiter limiter = new TpmRateLimiter(gatewayProperties, memoryLimiter, redisProvider);

        assertDoesNotThrow(() -> limiter.reserve(apiKey, 500));
    }

    private static ApiKey apiKey(long id, int tpmLimit) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setTpmLimit(tpmLimit);
        return apiKey;
    }
}