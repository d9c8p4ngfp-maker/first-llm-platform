package com.first.gateway.web.workspace.dto;

public record ProfileStatusResponse(
    boolean memoryEnabled,
    boolean profileEnabled,
    boolean profileInChat) {}
