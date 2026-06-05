package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProfileResult(String aiSummary, String aiTags, AiPersonality aiPersonality, String aiSystemPrompt) {}
