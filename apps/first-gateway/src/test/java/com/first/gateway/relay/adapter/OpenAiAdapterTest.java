package com.first.gateway.relay.adapter;

import com.first.gateway.infra.error.GatewayError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiAdapterTest {

    @Test
    void mapUpstreamStatus_maps429ToRateLimit() {
        assertEquals(GatewayError.RATE_LIMIT_EXCEEDED, OpenAiAdapter.mapUpstreamStatus(429));
    }

    @Test
    void mapUpstreamStatus_maps401ToUpstreamError() {
        assertEquals(GatewayError.UPSTREAM_ERROR, OpenAiAdapter.mapUpstreamStatus(401));
    }

    @Test
    void mapUpstreamStatus_maps500ToUpstreamError() {
        assertEquals(GatewayError.UPSTREAM_ERROR, OpenAiAdapter.mapUpstreamStatus(500));
    }

    @Test
    void mapUpstreamStatus_maps503ToUpstreamError() {
        assertEquals(GatewayError.UPSTREAM_ERROR, OpenAiAdapter.mapUpstreamStatus(503));
    }

    @Test
    void mapUpstreamStatus_maps402ToUpstreamError() {
        assertEquals(GatewayError.UPSTREAM_ERROR, OpenAiAdapter.mapUpstreamStatus(402));
    }
}