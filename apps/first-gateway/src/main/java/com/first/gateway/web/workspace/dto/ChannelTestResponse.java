package com.first.gateway.web.workspace.dto;

import java.util.Map;

public record ChannelTestResponse(boolean success, String message, Map<String, Object> data) {}
