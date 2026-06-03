package com.first.gateway.relay.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Parses token usage from OpenAI-compatible JSON and SSE chunks.
 */
public final class UsageParser {

    private final ObjectMapper objectMapper;

    public UsageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TokenUsage fromResponse(Map<String, Object> response) {
        Object usageObj = response.get("usage");
        if (!(usageObj instanceof Map<?, ?> usageMap)) {
            return TokenUsage.empty();
        }
        return new TokenUsage(
            intValue(usageMap.get("prompt_tokens")),
            intValue(usageMap.get("completion_tokens")),
            intValue(usageMap.get("total_tokens"))
        );
    }

    public TokenUsage fromStreamChunk(String chunk) {
        for (String line : chunk.split("\n")) {
            if (!line.startsWith("data:")) {
                continue;
            }
            String payload = line.substring(5).trim();
            if (payload.isEmpty() || "[DONE]".equals(payload)) {
                continue;
            }
            try {
                Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
                TokenUsage usage = fromResponse(event);
                if (usage.totalTokens() > 0) {
                    return usage;
                }
            } catch (Exception ignored) {
                // malformed SSE line — skip
            }
        }
        return TokenUsage.empty();
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
        public static TokenUsage empty() {
            return new TokenUsage(0, 0, 0);
        }
    }
}
