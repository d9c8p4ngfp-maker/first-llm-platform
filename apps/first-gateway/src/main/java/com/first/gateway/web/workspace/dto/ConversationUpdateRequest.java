package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationUpdateRequest(
    @NotBlank @Size(max = 200) String title
) {}