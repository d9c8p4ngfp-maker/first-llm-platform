package com.first.gateway.web.workspace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserMemoryRequest(
    String category,
    String content,
    LocalDate scheduleDate,
    String scheduleTime,
    Short importance,
    BigDecimal numericValue
) {}