package com.first.gateway.relay.dto;

public record Usage(int promptTokens, int completionTokens, int totalTokens) {}
