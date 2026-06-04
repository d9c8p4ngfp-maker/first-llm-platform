package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 2000) String description,
    @Size(max = 100) String suggestedModel
) {}