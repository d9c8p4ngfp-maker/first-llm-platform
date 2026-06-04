package com.first.gateway.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserMemoryRequest(
    @NotBlank @Size(max = 30) String category,
    @NotBlank @Size(max = 4000) String content,
    LocalDate scheduleDate,
    String scheduleTime,
    Short importance,
    BigDecimal numericValue
) {}