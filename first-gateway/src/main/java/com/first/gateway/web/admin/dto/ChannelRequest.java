package com.first.gateway.web.admin.dto;

import com.first.gateway.domain.enums.ChannelStatus;

public record ChannelRequest(
    String name,
    String type,
    String provider,
    String baseUrl,
    String apiKey,
    Integer priority,
    Integer weight,
    ChannelStatus status,
    Integer maxRpm,
    String config
) {}