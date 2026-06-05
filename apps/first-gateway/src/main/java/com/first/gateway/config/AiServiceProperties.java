package com.first.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-service")
public class AiServiceProperties {

    private boolean enabled = true;
    private boolean chat = true;
    private boolean memoryExtraction = true;
    private boolean profileSynthesis = true;
    private boolean rag = true;
    private String baseUrl = "http://localhost:8000";
    private String embeddingModel = "text-embedding-3-small";
    private String internalToken = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 300000;
    private int streamReadTimeoutMs = 300000;
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 2;
        private int backoffMs = 1000;
    }
}