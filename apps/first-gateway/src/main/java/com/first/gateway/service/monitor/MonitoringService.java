package com.first.gateway.service.monitor;

import com.first.gateway.infra.error.RateLimitExceededException;
import com.first.gateway.relay.router.CircuitState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MonitoringService {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> circuitStates = new ConcurrentHashMap<>();

    public MonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String model, String status, long durationMs) {
        Counter.builder("ai_request_total")
            .tag("model", safeTag(model))
            .tag("status", safeTag(status))
            .register(meterRegistry)
            .increment();
        Timer.builder("ai_request_duration_seconds")
            .tag("model", safeTag(model))
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordTokens(String type, long count) {
        if (count <= 0) {
            return;
        }
        Counter.builder("ai_token_total")
            .tag("type", safeTag(type))
            .register(meterRegistry)
            .increment(count);
    }

    public void recordChannelRequest(String channel, String status) {
        Counter.builder("ai_channel_request_total")
            .tag("channel", safeTag(channel))
            .tag("status", safeTag(status))
            .register(meterRegistry)
            .increment();
    }

    public void recordChannelLatency(String channel, long durationMs) {
        Timer.builder("ai_channel_latency_seconds")
            .tag("channel", safeTag(channel))
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRateLimit(RateLimitExceededException.LimitType limitType) {
        Counter.builder("ai_rate_limit_total")
            .tag("type", limitType.name().toLowerCase())
            .register(meterRegistry)
            .increment();
    }

    public void updateCircuitState(String channel, CircuitState state) {
        AtomicInteger gaugeValue = circuitStates.computeIfAbsent(channel, key -> {
            AtomicInteger value = new AtomicInteger(circuitGaugeValue(state));
            Gauge.builder("ai_channel_circuit_state", value, AtomicInteger::get)
                .tag("channel", safeTag(key))
                .register(meterRegistry);
            return value;
        });
        gaugeValue.set(circuitGaugeValue(state));
    }

    private static int circuitGaugeValue(CircuitState state) {
        return switch (state) {
            case OPEN -> 1;
            case HALF_OPEN -> 2;
            case CLOSED -> 0;
        };
    }

    private static String safeTag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
