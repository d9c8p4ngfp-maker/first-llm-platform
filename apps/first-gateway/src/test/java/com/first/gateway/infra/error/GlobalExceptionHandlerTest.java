package com.first.gateway.infra.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MDC.clear();
    }

    @Test
    void handleGateway_masksUpstreamDetail() {
        GatewayException ex = GatewayException.withInternal(
            GatewayError.UPSTREAM_ERROR, "upstream 401: secret");

        ResponseEntity<Map<String, Object>> response = handler.handleGateway(ex);

        assertEquals(502, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) response.getBody().get("error");
        assertEquals("上游服务异常", error.get("message"));
        assertEquals("upstream_error", error.get("code"));
    }

    @Test
    void handleGateway_returnsClientDetailForInvalidRequest() {
        GatewayException ex = new GatewayException(GatewayError.INVALID_REQUEST, "model is required");

        ResponseEntity<Map<String, Object>> response = handler.handleGateway(ex);

        assertEquals(400, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) response.getBody().get("error");
        assertEquals("model is required", error.get("message"));
    }

    @Test
    void handleAll_returnsInternalErrorWithoutDetails() {
        ResponseEntity<Map<String, Object>> response = handler.handleAll(new RuntimeException("db leak"));

        assertEquals(500, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) response.getBody().get("error");
        assertEquals("系统内部错误", error.get("message"));
        assertNull(error.get("param"));
    }
}
