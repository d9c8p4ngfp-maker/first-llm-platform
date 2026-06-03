package com.first.gateway.web.workspace.dto;

import java.util.Map;

public record PromptPreviewRequest(Map<String, Object> variables) {}