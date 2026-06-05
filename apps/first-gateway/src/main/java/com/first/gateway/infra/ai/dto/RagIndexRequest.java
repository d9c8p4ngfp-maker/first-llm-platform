package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RagIndexRequest(
    Long documentId,
    Long knowledgeBaseId,
    String content,
    String filePath,
    String fileType,
    String embeddingModel,
    UpstreamConfig upstream
) {}
