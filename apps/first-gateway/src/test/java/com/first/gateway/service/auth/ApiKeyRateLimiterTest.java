package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiKeyRateLimiterTest {

    private MemoryApiKeyRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new MemoryApiKeyRateLimiter();
    }

    @Test
    void check_skipsWhenLimitDisabled() {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(1L);
        apiKey.setRateLimit(-1);

        assertDoesNotThrow(() -> rateLimiter.check(apiKey));
    }

    @Test
    void check_rejectsWhenLimitExceeded() {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(99L);
        apiKey.setRateLimit(2);

        rateLimiter.check(apiKey);
        rateLimiter.check(apiKey);

        GatewayException ex = assertThrows(GatewayException.class, () -> rateLimiter.check(apiKey));
        assertEquals(GatewayError.RATE_LIMIT_EXCEEDED, ex.getError());
    }
}
