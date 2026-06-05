package com.first.gateway.infra.ai.dto;

public record ModelConfig(String model, ModelParams modelParams, String promptOverride) {}
