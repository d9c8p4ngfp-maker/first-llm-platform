package com.first.gateway.web.workspace.dto;

public record ScheduleEntryResponse(Long id, String date, String time, String content, String status, String category) {}
