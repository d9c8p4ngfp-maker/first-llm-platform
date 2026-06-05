package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChatRequest(
    String model,
    List<ChatMessage> messages,
    boolean stream,
    ModelParams modelParams,
    UserProfileContext userProfile,
    List<MemoryContext> userMemories,
    List<RagChunkResult> ragContext,
    List<Map<String, Object>> tools,
    UpstreamConfig upstream
) {}
