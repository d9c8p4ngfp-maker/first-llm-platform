package com.first.gateway.web.workspace.dto;

public record AiHealthStatusResponse(boolean enabled, boolean chat, boolean memoryExtraction, boolean profileSynthesis, boolean rag) {}
