package com.first.gateway.web.workspace.dto;

public record McpServerRequest(
    String name,
    String endpoint,
    String transport,
    String serverType,
    String command,
    String envConfig
) {}