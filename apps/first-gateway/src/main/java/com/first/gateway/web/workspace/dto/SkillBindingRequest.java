package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SkillBindingRequest(
    @NotBlank String type,
    @NotNull Long bindingId
) {}