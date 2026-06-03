package com.first.gateway.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Size(min = 3, max = 100) String username,
    @NotBlank @Size(min = 6, max = 100) String password,
    String email,
    Long groupId
) {}
