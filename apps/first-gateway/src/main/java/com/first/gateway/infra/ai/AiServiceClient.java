package com.first.gateway.infra.ai;

import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.infra.ai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;

@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final AiServiceProperties properties;
    private final WebClient webClient;
    private final AtomicBoolean healthyCache = new AtomicBoolean(false);

    public AiServiceClient(AiServiceProperties properties, WebClient aiServiceWebClient) {
        this.properties = properties;
        this.webClient = aiServiceWebClient;
    }

    @PostConstruct
    void initHealthCache() {
        refreshHealthStatus();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isChatEnabled() {
        return properties.isEnabled() && properties.isChat();
    }

    public boolean isMemoryExtractionEnabled() {
        return properties.isEnabled() && properties.isMemoryExtraction();
    }

    public boolean isProfileSynthesisEnabled() {
        return properties.isEnabled() && properties.isProfileSynthesis();
    }

    public boolean isRagEnabled() {
        return properties.isEnabled() && properties.isRag();
    }

    public String baseUrl() {
        return properties.getBaseUrl();
    }

    public Optional<Map<String, Object>> fetchHealth() {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        int attempts = Math.max(1, properties.getRetry().getMaxAttempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Map<String, Object> body = webClient.get()
                    .uri("/health")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofMillis(properties.getReadTimeoutMs()));
                return Optional.ofNullable(body);
            } catch (WebClientRequestException | WebClientResponseException ex) {
                log.warn("AI service health check failed (attempt {}/{}): {}",
                    attempt, attempts, ex.getMessage());
                if (attempt < attempts) {
                    sleep(properties.getRetry().getBackoffMs());
                }
            } catch (Exception ex) {
                log.warn("AI service health check error: {}", ex.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public boolean isHealthy() {
        return healthyCache.get();
    }

    @Scheduled(fixedRate = 3000)
    void refreshHealthStatus() {
        if (!properties.isEnabled()) {
            healthyCache.set(false);
            return;
        }
        boolean result = fetchHealth()
            .map(body -> {
                Object healthy = body.get("healthy");
                if (healthy instanceof Boolean b) return b;
                return "ok".equalsIgnoreCase(String.valueOf(body.get("status")));
            })
            .orElse(false);
        healthyCache.set(result);
    }

    public Optional<Map<String, Object>> chat(ChatRequest request) {
        if (!isChatEnabled()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = webClient.post()
                .uri("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI chat call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean chatStream(ChatRequest request, Consumer<String> chunkConsumer) {
        if (!isChatEnabled()) {
            return false;
        }
        try {
            Flux<DataBuffer> flux = webClient.post()
                .uri("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(DataBuffer.class);

            StringBuilder pending = new StringBuilder();
            flux.doOnNext(buffer -> {
                pending.append(buffer.toString(StandardCharsets.UTF_8));
                DataBufferUtils.release(buffer);
                flushLines(pending, chunkConsumer);
            }).doOnComplete(() -> {
                if (!pending.isEmpty()) {
                    chunkConsumer.accept(pending.toString());
                }
            }).blockLast(Duration.ofMillis(properties.getStreamReadTimeoutMs()));
            return true;
        } catch (Exception ex) {
            log.warn("AI chat stream failed: {}", ex.getMessage());
            return false;
        }
    }

    public Optional<MemoryExtractResponse> extractMemories(MemoryExtractRequest request) {
        if (!isMemoryExtractionEnabled()) {
            return Optional.empty();
        }
        try {
            MemoryExtractResponse body = webClient.post()
                .uri("/ai/memory/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MemoryExtractResponse.class)
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI memory extraction call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ProfileSynthesizeResponse> synthesizeProfile(ProfileSynthesizeRequest request) {
        if (!isProfileSynthesisEnabled()) {
            return Optional.empty();
        }
        try {
            ProfileSynthesizeResponse body = webClient.post()
                .uri("/ai/profile/synthesize")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProfileSynthesizeResponse.class)
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI profile synthesis failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<RagQueryResponse> queryRag(RagQueryRequest request) {
        if (!isRagEnabled()) {
            return Optional.empty();
        }
        try {
            RagQueryResponse body = webClient.post()
                .uri("/ai/rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RagQueryResponse.class)
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI RAG query failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<EmbedResponse> embed(EmbedRequest request) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            EmbedResponse body = webClient.post()
                .uri("/ai/rag/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbedResponse.class)
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI embed call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<RagIndexResponse> indexRag(RagIndexRequest request) {
        if (!isRagEnabled()) {
            return Optional.empty();
        }
        try {
            RagIndexResponse body = webClient.post()
                .uri("/ai/rag/index")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RagIndexResponse.class)
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI RAG index failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<CrawlAndIndexResponse> crawlAndIndex(CrawlAndIndexRequest request) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            CrawlAndIndexResponse body = webClient.post()
                .uri("/ai/rag/crawl")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CrawlAndIndexResponse.class)
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI crawl-and-index failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static void flushLines(StringBuilder pending, Consumer<String> chunkConsumer) {
        int idx;
        while ((idx = pending.indexOf("\n")) >= 0) {
            String line = pending.substring(0, idx + 1);
            pending.delete(0, idx + 1);
            chunkConsumer.accept(line);
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}