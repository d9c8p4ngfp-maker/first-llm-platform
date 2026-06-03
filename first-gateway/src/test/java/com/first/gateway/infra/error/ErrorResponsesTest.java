package com.first.gateway.infra.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ErrorResponsesTest {

    @Test
    void clientMessage_hidesUpstreamInternalDetail() {
        GatewayException ex = GatewayException.withInternal(
            GatewayError.UPSTREAM_ERROR, "upstream 401: secret body");

        assertEquals("上游服务异常", ErrorResponses.clientMessage(ex));
    }

    @Test
    void clientMessage_keepsValidationDetail() {
        GatewayException ex = new GatewayException(GatewayError.INVALID_REQUEST, "model is required");

        assertEquals("model is required", ErrorResponses.clientMessage(ex));
    }

    @Test
    void body_includesErrorCodeAndType() {
        var body = ErrorResponses.body(GatewayError.INVALID_API_KEY, null);

        @SuppressWarnings("unchecked")
        var error = (java.util.Map<String, Object>) body.get("error");
        assertEquals("invalid_api_key", error.get("code"));
        assertEquals("invalid_api_key", error.get("type"));
        assertFalse(body.containsKey("request_id"));
    }
}
