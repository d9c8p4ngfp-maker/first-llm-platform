package com.first.gateway.web.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemCodeRequest(@NotBlank String code) {}
