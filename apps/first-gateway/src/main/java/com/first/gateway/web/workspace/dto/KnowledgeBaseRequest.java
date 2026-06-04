package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeBaseRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description
) {}