package com.first.gateway.service.monitor;

import com.first.gateway.relay.router.CircuitState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringServiceTest {

    private SimpleMeterRegistry meterRegistry;
    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        monitoringService = new MonitoringService(meterRegistry);
    }

    @Test
    void recordRequest_incrementsCounter() {
        monitoringService.recordRequest("gpt-4", "success", 100);

        assertEquals(1.0, meterRegistry.get("ai_request_total")
            .tag("model", "gpt-4")
            .tag("status", "success")
            .counter()
            .count());
    }

    @Test
    void recordRequest_recordsDuration() {
        monitoringService.recordRequest("gpt-4", "success", 1500);

        assertEquals(1, meterRegistry.get("ai_request_duration_seconds")
            .tag("model", "gpt-4")
            .timer()
            .count());
    }

    @Test
    void recordTokens_incrementsByType() {
        monitoringService.recordTokens("prompt", 100);
        monitoringService.recordTokens("completion", 200);

        assertEquals(100.0, meterRegistry.get("ai_token_total").tag("type", "prompt").counter().count());
        assertEquals(200.0, meterRegistry.get("ai_token_total").tag("type", "completion").counter().count());
    }

    @Test
    void recordChannelRequest_taggedCorrectly() {
        monitoringService.recordChannelRequest("openai_primary", "success");

        assertEquals(1.0, meterRegistry.get("ai_channel_request_total")
            .tag("channel", "openai_primary")
            .tag("status", "success")
            .counter()
            .count());
    }

    @Test
    void noHighCardinalityLabels() {
        monitoringService.recordRequest("deepseek-chat", "success", 50);

        meterRegistry.getMeters().stream()
            .filter(m -> m.getId().getName().startsWith("ai_"))
            .flatMap(m -> m.getId().getTags().stream())
            .map(tag -> tag.getKey())
            .forEach(key -> assertTrue(
                !key.equals("user_id") && !key.equals("api_key_id"),
                "unexpected high-cardinality tag: " + key));
    }

    @Test
    void updateCircuitState_setsGauge() {
        monitoringService.updateCircuitState("channel-a", CircuitState.OPEN);

        assertEquals(1.0, meterRegistry.get("ai_channel_circuit_state")
            .tag("channel", "channel-a")
            .gauge()
            .value());
    }
}