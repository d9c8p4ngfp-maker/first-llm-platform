package com.first.gateway.relay.router;

import com.first.gateway.config.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChannelCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(ChannelCircuitBreaker.class);
    private static final String CIRCUIT_KEY = "channel:circuit:%d";
    private static final long REDIS_TTL_SECONDS = 300;

    private final GatewayProperties gatewayProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ConcurrentHashMap<Long, MemoryCircuit> memoryCircuits = new ConcurrentHashMap<>();

    public ChannelCircuitBreaker(GatewayProperties gatewayProperties,
                                 ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.gatewayProperties = gatewayProperties;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public boolean isOpen(Long channelId) {
        if (channelId == null) {
            return false;
        }
        CircuitSnapshot snapshot = readSnapshot(channelId);
        return snapshot.state() == CircuitState.OPEN;
    }

    public CircuitState getState(Long channelId) {
        return readSnapshot(channelId).state();
    }

    public void recordSuccess(Long channelId) {
        if (channelId == null) {
            return;
        }
        if (updateRedisSuccess(channelId)) {
            return;
        }
        updateMemorySuccess(channelId);
    }

    public void recordFailure(Long channelId) {
        if (channelId == null) {
            return;
        }
        if (updateRedisFailure(channelId)) {
            return;
        }
        updateMemoryFailure(channelId);
    }

    private CircuitSnapshot readSnapshot(Long channelId) {
        CircuitSnapshot redisSnapshot = readRedisSnapshot(channelId);
        if (redisSnapshot != null) {
            return redisSnapshot;
        }
        return readMemorySnapshot(channelId);
    }

    private CircuitSnapshot readRedisSnapshot(Long channelId) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            Map<Object, Object> entries = redis.opsForHash().entries(CIRCUIT_KEY.formatted(channelId));
            if (entries.isEmpty()) {
                return new CircuitSnapshot(CircuitState.CLOSED, 0, null);
            }
            CircuitState state = CircuitState.valueOf(String.valueOf(entries.getOrDefault("state", "CLOSED")));
            int failureCount = Integer.parseInt(String.valueOf(entries.getOrDefault("failure_count", "0")));
            Instant openedAt = parseInstant(entries.get("opened_at"));
            return normalizeSnapshot(state, failureCount, openedAt);
        } catch (Exception ex) {
            log.warn("redis circuit read failed, fallback to memory: {}", ex.getMessage());
            return null;
        }
    }

    private boolean updateRedisSuccess(Long channelId) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            String key = CIRCUIT_KEY.formatted(channelId);
            CircuitSnapshot snapshot = readRedisSnapshot(channelId);
            if (snapshot == null) {
                return false;
            }
            if (snapshot.state() == CircuitState.HALF_OPEN) {
                redis.opsForHash().put(key, "state", CircuitState.CLOSED.name());
                redis.opsForHash().put(key, "failure_count", "0");
                redis.opsForHash().delete(key, "opened_at");
            } else {
                redis.opsForHash().put(key, "failure_count", "0");
            }
            redis.expire(key, java.time.Duration.ofSeconds(REDIS_TTL_SECONDS));
            return true;
        } catch (Exception ex) {
            log.warn("redis circuit success update failed, fallback to memory: {}", ex.getMessage());
            return false;
        }
    }

    private boolean updateRedisFailure(Long channelId) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            String key = CIRCUIT_KEY.formatted(channelId);
            CircuitSnapshot snapshot = readRedisSnapshot(channelId);
            if (snapshot == null) {
                return false;
            }
            if (snapshot.state() == CircuitState.HALF_OPEN) {
                Instant now = Instant.now();
                redis.opsForHash().put(key, "state", CircuitState.OPEN.name());
                redis.opsForHash().put(key, "opened_at", String.valueOf(now.getEpochSecond()));
                redis.opsForHash().put(key, "failure_count", String.valueOf(snapshot.failureCount() + 1));
            } else {
                int failures = snapshot.failureCount() + 1;
                redis.opsForHash().put(key, "failure_count", String.valueOf(failures));
                if (failures >= failureThreshold()) {
                    redis.opsForHash().put(key, "state", CircuitState.OPEN.name());
                    redis.opsForHash().put(key, "opened_at", String.valueOf(Instant.now().getEpochSecond()));
                } else if (!redis.hasKey(key)) {
                    redis.opsForHash().put(key, "state", CircuitState.CLOSED.name());
                }
            }
            redis.expire(key, java.time.Duration.ofSeconds(REDIS_TTL_SECONDS));
            return true;
        } catch (Exception ex) {
            log.warn("redis circuit failure update failed, fallback to memory: {}", ex.getMessage());
            return false;
        }
    }

    private CircuitSnapshot readMemorySnapshot(Long channelId) {
        MemoryCircuit circuit = memoryCircuits.computeIfAbsent(channelId, id -> new MemoryCircuit());
        synchronized (circuit) {
            return normalizeSnapshot(circuit.state, circuit.failureCount, circuit.openedAt);
        }
    }

    private void updateMemorySuccess(Long channelId) {
        MemoryCircuit circuit = memoryCircuits.computeIfAbsent(channelId, id -> new MemoryCircuit());
        synchronized (circuit) {
            CircuitState effective = normalizeSnapshot(circuit.state, circuit.failureCount, circuit.openedAt).state();
            if (effective == CircuitState.HALF_OPEN || circuit.state == CircuitState.HALF_OPEN) {
                circuit.state = CircuitState.CLOSED;
                circuit.openedAt = null;
            }
            circuit.failureCount = 0;
        }
    }

    private void updateMemoryFailure(Long channelId) {
        MemoryCircuit circuit = memoryCircuits.computeIfAbsent(channelId, id -> new MemoryCircuit());
        synchronized (circuit) {
            CircuitState effective = normalizeSnapshot(circuit.state, circuit.failureCount, circuit.openedAt).state();
            if (effective == CircuitState.HALF_OPEN || circuit.state == CircuitState.HALF_OPEN) {
                circuit.state = CircuitState.OPEN;
                circuit.openedAt = Instant.now();
                circuit.failureCount++;
                return;
            }
            circuit.failureCount++;
            if (circuit.failureCount >= failureThreshold()) {
                circuit.state = CircuitState.OPEN;
                circuit.openedAt = Instant.now();
            }
        }
    }

    private CircuitSnapshot normalizeSnapshot(CircuitState state, int failureCount, Instant openedAt) {
        if (state == CircuitState.OPEN && openedAt != null) {
            long elapsed = Instant.now().getEpochSecond() - openedAt.getEpochSecond();
            if (elapsed >= openDurationSeconds()) {
                return new CircuitSnapshot(CircuitState.HALF_OPEN, failureCount, openedAt);
            }
        }
        return new CircuitSnapshot(state, failureCount, openedAt);
    }

    private int failureThreshold() {
        return Math.max(1, gatewayProperties.getCircuitBreaker().getFailureThreshold());
    }

    private int openDurationSeconds() {
        return Math.max(1, gatewayProperties.getCircuitBreaker().getOpenDurationSeconds());
    }

    private static Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        return Instant.ofEpochSecond(Long.parseLong(String.valueOf(value)));
    }

    private record CircuitSnapshot(CircuitState state, int failureCount, Instant openedAt) {}

    private static final class MemoryCircuit {
        private CircuitState state = CircuitState.CLOSED;
        private int failureCount;
        private Instant openedAt;
    }
}
