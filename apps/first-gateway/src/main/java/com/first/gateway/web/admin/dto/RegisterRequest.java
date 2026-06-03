package com.first.gateway.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username length must be 3-50 characters")
    String username,
    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100, message = "password must be at least 6 characters")
    String password,
    String email
) {}