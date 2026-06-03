package com.first.gateway.web.workspace.dto;

import java.util.Map;

public record PipelinePreviewRequest(Map<String, Object> variables) {}