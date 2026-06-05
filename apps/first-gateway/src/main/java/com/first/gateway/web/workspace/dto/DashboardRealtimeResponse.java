package com.first.gateway.web.workspace.dto;

import java.math.BigDecimal;

public record DashboardRealtimeResponse(long totalRequests, BigDecimal totalCost, long activeUsers, TodayStats today) {}
