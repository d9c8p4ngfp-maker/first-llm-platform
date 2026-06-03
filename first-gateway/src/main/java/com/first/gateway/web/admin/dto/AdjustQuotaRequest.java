package com.first.gateway.web.admin.dto;

import jakarta.validation.constraints.Min;

public record AdjustQuotaRequest(@Min(0) long totalTokens) {}
