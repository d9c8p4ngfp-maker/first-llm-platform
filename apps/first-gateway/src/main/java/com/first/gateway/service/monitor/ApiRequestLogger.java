package com.first.gateway.service.monitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ApiRequestLogger {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLogger.class);
    private static final int PROMPT_MAX_LENGTH = 100;

    private final ObjectMapper objectMapper;

    public ApiRequestLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void log(String requestId,
                    String model,
                    String apiKeyPrefix,
                    int promptTokens,
                    int completionTokens,
                    int totalTokens,
                    long latencyMs,
                    String status,
                    String promptPreview) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "api_request");
        payload.put("request_id", requestId);
        payload.put("model", model);
        payload.put("api_key_prefix", apiKeyPrefix);
        payload.put("prompt_tokens", promptTokens);
        payload.put("completion_tokens", completionTokens);
        payload.put("tokens", totalTokens);
        payload.put("latency_ms", latencyMs);
        payload.put("status", status);
        if (promptPreview != null) {
            payload.put("prompt_preview", truncate(promptPreview));
        }
        try {
            log.info(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            log.info("type=api_request request_id={} model={} status={}", requestId, model, status);
        }
    }

    static String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= PROMPT_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, PROMPT_MAX_LENGTH);
    }
}
