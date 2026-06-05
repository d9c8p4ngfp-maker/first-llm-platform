package com.first.gateway.web.workspace.dto;

import java.math.BigDecimal;

public record TodayStats(long requests, BigDecimal cost, long users) {}
