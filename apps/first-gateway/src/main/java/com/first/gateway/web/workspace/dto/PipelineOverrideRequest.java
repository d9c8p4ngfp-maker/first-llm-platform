package com.first.gateway.web.workspace.dto;

public record PipelineOverrideRequest(
    String modelId,
    String modelParams,
    Long promptTemplateId,
    String promptText,
    Short enabled
) {}