package com.first.gateway.relay.router;

import com.first.gateway.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelCircuitBreakerTest {

    private static final long CHANNEL_ID = 42L;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private GatewayProperties gatewayProperties;
    private ChannelCircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);
        gatewayProperties = new GatewayProperties();
        gatewayProperties.getCircuitBreaker().setFailureThreshold(5);
        gatewayProperties.getCircuitBreaker().setOpenDurationSeconds(30);
        circuitBreaker = new ChannelCircuitBreaker(gatewayProperties, redisTemplateProvider);
    }

    @Test
    void initialState_isClosed() {
        assertFalse(circuitBreaker.isOpen(CHANNEL_ID));
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState(CHANNEL_ID));
    }

    @Test
    void recordFailure_belowThreshold_staysClosed() {
        for (int i = 0; i < 4; i++) {
            circuitBreaker.recordFailure(CHANNEL_ID);
        }

        assertFalse(circuitBreaker.isOpen(CHANNEL_ID));
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState(CHANNEL_ID));
    }

    @Test
    void recordFailure_atThreshold_opensCircuit() {
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure(CHANNEL_ID);
        }

        assertTrue(circuitBreaker.isOpen(CHANNEL_ID));
        assertEquals(CircuitState.OPEN, circuitBreaker.getState(CHANNEL_ID));
    }

    @Test
    void openState_rejectsTraffic() {
        openCircuit();

        assertTrue(circuitBreaker.isOpen(CHANNEL_ID));
    }

    @Test
    void openState_afterTimeout_transitionsToHalfOpen() {
        openCircuit();
        setOpenedAtSecondsAgo(31);

        assertFalse(circuitBreaker.isOpen(CHANNEL_ID));
        assertEquals(CircuitState.HALF_OPEN, circuitBreaker.getState(CHANNEL_ID));
    }

    @Test
    void halfOpen_successfulProbe_closesCircuit() {
        openCircuit();
        setOpenedAtSecondsAgo(31);
        assertEquals(CircuitState.HALF_OPEN, circuitBreaker.getState(CHANNEL_ID));

        circuitBreaker.recordSuccess(CHANNEL_ID);

        assertFalse(circuitBreaker.isOpen(CHANNEL_ID));
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState(CHANNEL_ID));
    }

    @Test
    void halfOpen_failedProbe_reopensCircuit() {
        openCircuit();
        setOpenedAtSecondsAgo(31);
        assertEquals(CircuitState.HALF_OPEN, circuitBreaker.getState(CHANNEL_ID));

        circuitBreaker.recordFailure(CHANNEL_ID);

        assertTrue(circuitBreaker.isOpen(CHANNEL_ID));
        assertEquals(CircuitState.OPEN, circuitBreaker.getState(CHANNEL_ID));
    }

    @Test
    void redisTTL_autoRecovery() {
        openCircuit();
        setOpenedAtSecondsAgo(31);

        assertFalse(circuitBreaker.isOpen(CHANNEL_ID));
    }

    private void openCircuit() {
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure(CHANNEL_ID);
        }
    }

    private void setOpenedAtSecondsAgo(long secondsAgo) {
        try {
            Field memoryField = ChannelCircuitBreaker.class.getDeclaredField("memoryCircuits");
            memoryField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Long, Object> circuits =
                (ConcurrentHashMap<Long, Object>) memoryField.get(circuitBreaker);
            Object circuit = circuits.get(CHANNEL_ID);
            Field openedAtField = circuit.getClass().getDeclaredField("openedAt");
            openedAtField.setAccessible(true);
            openedAtField.set(circuit, Instant.now().minusSeconds(secondsAgo));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}