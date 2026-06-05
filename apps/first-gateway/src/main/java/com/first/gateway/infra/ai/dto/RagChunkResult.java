package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RagChunkResult(
    Long documentId,
    Long knowledgeBaseId,
    String content,
    double score,
    Map<String, Object> metadata
) {}
