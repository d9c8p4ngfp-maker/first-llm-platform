package com.first.gateway.infra.ai.dto;

public record SynthesisConfig(String model, ModelParams modelParams, String promptOverride) {}
