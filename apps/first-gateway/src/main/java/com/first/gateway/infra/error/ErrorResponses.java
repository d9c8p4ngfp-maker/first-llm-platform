package com.first.gateway.infra.error;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ErrorResponses {

    private static final Set<GatewayError> HIDE_CLIENT_DETAIL = Set.of(
        GatewayError.UPSTREAM_ERROR,
        GatewayError.UPSTREAM_TIMEOUT,
        GatewayError.INTERNAL_ERROR,
        GatewayError.SERVICE_UNAVAILABLE
    );

    private ErrorResponses() {}

    public static String clientMessage(GatewayException ex) {
        GatewayError error = ex.getError();
        if (HIDE_CLIENT_DETAIL.contains(error)) {
            return error.getDefaultMessage();
        }
        if (ex.getDetail() != null) {
            return ex.getDetail();
        }
        return error.getDefaultMessage();
    }

    public static Map<String, Object> body(GatewayError error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("message", message != null ? message : error.getDefaultMessage());
        err.put("type", error.getCode());
        err.put("code", error.getCode());
        err.put("param", null);
        body.put("error", err);
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            body.put("request_id", traceId);
        }
        return body;
    }

    public static Map<String, Object> body(GatewayException ex) {
        return body(ex.getError(), clientMessage(ex));
    }
}