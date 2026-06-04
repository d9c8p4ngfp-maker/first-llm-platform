package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModelPreferencesRequest(
    @NotBlank @Size(max = 100) String defaultModel
) {}