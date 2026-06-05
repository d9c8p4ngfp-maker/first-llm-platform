package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MemoryExtractRequest(
    List<ChatMessage> conversationSegment,
    List<ExistingMemoryRef> existingMemories,
    ModelConfig config,
    UpstreamConfig upstream
) {}
