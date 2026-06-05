package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExtractedMemory(
    String category, String content, Integer importance,
    String scheduleDate, String scheduleTime,
    Double numericValue, String unit
) {}
