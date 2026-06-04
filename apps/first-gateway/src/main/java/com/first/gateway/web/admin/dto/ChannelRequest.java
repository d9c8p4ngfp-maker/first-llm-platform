package com.first.gateway.web.admin.dto;

import com.first.gateway.domain.enums.ChannelStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChannelRequest(
    @NotBlank(groups = OnCreate.class) @Size(max = 100) String name,
    @Size(max = 50) String type,
    @Size(max = 50) String provider,
    @NotBlank(groups = OnCreate.class) @Size(max = 500) String baseUrl,
    @NotBlank(groups = OnCreate.class) @Size(max = 500) String apiKey,
    Integer priority,
    Integer weight,
    ChannelStatus status,
    Integer maxRpm,
    @Size(max = 4096) String config
) {
    public interface OnCreate {}
}
