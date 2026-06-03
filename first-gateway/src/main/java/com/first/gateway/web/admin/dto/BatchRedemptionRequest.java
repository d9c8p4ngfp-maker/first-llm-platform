package com.first.gateway.web.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record BatchRedemptionRequest(
    @Min(1) @Max(1000) int count,
    @Min(1) long amount,
    Instant expiresAt
) {}
