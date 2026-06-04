package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentRequest(
    @NotBlank @Size(max = 500) String title,
    @NotBlank @Size(max = 100000) String content
) {}