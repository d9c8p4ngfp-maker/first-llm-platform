package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationMessageRequest(
    @NotBlank @Size(max = 20) String role,
    @NotBlank @Size(max = 32000) String content
) {}