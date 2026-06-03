package com.first.gateway.relay.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageParserTest {

    private UsageParser parser;

    @BeforeEach
    void setUp() {
        parser = new UsageParser(new ObjectMapper());
    }

    @Test
    void fromResponse_readsOpenAiUsageBlock() {
        Map<String, Object> response = Map.of(
            "usage", Map.of(
                "prompt_tokens", 10,
                "completion_tokens", 20,
                "total_tokens", 30
            )
        );

        UsageParser.TokenUsage usage = parser.fromResponse(response);

        assertEquals(10, usage.promptTokens());
        assertEquals(20, usage.completionTokens());
        assertEquals(30, usage.totalTokens());
    }

    @Test
    void fromResponse_returnsEmptyWhenUsageMissing() {
        UsageParser.TokenUsage usage = parser.fromResponse(Map.of("id", "chatcmpl-1"));

        assertEquals(0, usage.totalTokens());
    }

    @Test
    void fromStreamChunk_readsUsageFromSseDataLine() {
        String chunk = """
            data: {"choices":[],"usage":{"prompt_tokens":5,"completion_tokens":7,"total_tokens":12}}

            """;

        UsageParser.TokenUsage usage = parser.fromStreamChunk(chunk);

        assertEquals(12, usage.totalTokens());
    }

    @Test
    void fromStreamChunk_ignoresDoneMarker() {
        String chunk = "data: [DONE]\n\n";

        UsageParser.TokenUsage usage = parser.fromStreamChunk(chunk);

        assertEquals(0, usage.totalTokens());
    }
}
