package com.first.gateway.relay.router;

import com.first.gateway.config.GatewayProperties;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyTest {

    private RetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {
        GatewayProperties properties = new GatewayProperties();
        properties.getRetry().setMaxAttempts(3);
        retryPolicy = new RetryPolicy(properties);
    }

    @Test
    void shouldRetry_allowsUpstreamErrorsWithinLimit() {
        GatewayException ex = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "boom");
        assertTrue(retryPolicy.shouldRetry(1, ex));
    }

    @Test
    void shouldRetry_rejectsWhenAttemptsExhausted() {
        GatewayException ex = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "boom");
        assertFalse(retryPolicy.shouldRetry(3, ex));
    }

    @Test
    void shouldRetry_rejectsNonRetryableErrors() {
        GatewayException ex = new GatewayException(GatewayError.MODEL_NOT_FOUND);
        assertFalse(retryPolicy.shouldRetry(1, ex));
    }

    @Test
    void shouldRetry_allowsUpstreamTimeoutWithinLimit() {
        GatewayException ex = GatewayException.withInternal(GatewayError.UPSTREAM_TIMEOUT, "timed out");
        assertTrue(retryPolicy.shouldRetry(1, ex));
    }

    @Test
    void shouldRetry_allowsRateLimitExceededWithinLimit() {
        GatewayException ex = GatewayException.withInternal(GatewayError.RATE_LIMIT_EXCEEDED, "429");
        assertTrue(retryPolicy.shouldRetry(1, ex));
    }

    @Test
    void shouldRetry_rejectsNonGatewayException() {
        assertFalse(retryPolicy.shouldRetry(1, new RuntimeException("random")));
    }

    @Test
    void shouldRetry_rejectsClientErrors() {
        assertFalse(retryPolicy.shouldRetry(1, new GatewayException(GatewayError.INVALID_API_KEY)));
        assertFalse(retryPolicy.shouldRetry(1, new GatewayException(GatewayError.INSUFFICIENT_QUOTA)));
        assertFalse(retryPolicy.shouldRetry(1, new GatewayException(GatewayError.INVALID_REQUEST)));
    }
}