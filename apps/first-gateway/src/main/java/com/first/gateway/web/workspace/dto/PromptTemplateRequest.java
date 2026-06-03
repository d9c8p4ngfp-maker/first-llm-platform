package com.first.gateway.web.workspace.dto;

public record PromptTemplateRequest(
    String name,
    String description,
    String industry,
    String category,
    String visibility,
    String systemPrompt,
    String userPromptTemplate,
    String variables,
    String suggestedModel
) {}