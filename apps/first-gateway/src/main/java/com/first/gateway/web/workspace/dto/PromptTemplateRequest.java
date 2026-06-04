package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromptTemplateRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    @Size(max = 100) String industry,
    @Size(max = 100) String category,
    @Size(max = 20) String visibility,
    String systemPrompt,
    String userPromptTemplate,
    String variables,
    @Size(max = 100) String suggestedModel
) {}