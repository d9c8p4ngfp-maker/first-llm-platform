package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CrawlAndIndexRequest(
    String url, Long knowledgeBaseId, Long documentId,
    String embeddingModel, UpstreamConfig upstream
) {}
