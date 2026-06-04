package com.first.gateway.service.auth.admin;

import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistStoreTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    private JwtBlacklistStore store;

    @BeforeEach
    void setUp() {
        store = new JwtBlacklistStore(redis);
    }

    @Test
    void push_writesKeyWithTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);

        store.push("jti-1", 60_000);

        verify(valueOps).set(eq("jwt:blacklist:jti-1"), eq("1"), eq(Duration.ofMillis(60_000)));
    }

    @Test
    void push_redisFailure_throwsServiceUnavailable() {
        when(redis.opsForValue()).thenReturn(valueOps);
        doThrow(new RuntimeException("connection refused"))
            .when(valueOps).set(any(), any(), any(Duration.class));

        GatewayException ex = assertThrows(GatewayException.class, () -> store.push("jti-1", 60_000));

        assertEquals(GatewayError.SERVICE_UNAVAILABLE, ex.getError());
    }

    @Test
    void contains_returnsTrueWhenKeyExists() {
        when(redis.hasKey("jwt:blacklist:jti-1")).thenReturn(true);

        assertTrue(store.contains("jti-1"));
    }

    @Test
    void contains_redisFailure_throwsServiceUnavailable() {
        when(redis.hasKey("jwt:blacklist:jti-1")).thenThrow(new RuntimeException("connection refused"));

        GatewayException ex = assertThrows(GatewayException.class, () -> store.contains("jti-1"));

        assertEquals(GatewayError.SERVICE_UNAVAILABLE, ex.getError());
    }
}
