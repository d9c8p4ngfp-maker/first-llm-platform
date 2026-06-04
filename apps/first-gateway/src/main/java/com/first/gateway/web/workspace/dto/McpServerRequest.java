package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record McpServerRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String endpoint,
    String transport,
    String serverType,
    String command,
    String envConfig
) {}