package com.first.gateway.infra.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final AiServiceProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AiServiceClient(AiServiceProperties properties, WebClient aiServiceWebClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.webClient = aiServiceWebClient;
        this.objectMapper = objectMapper;
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
        return fetchHealth()
            .map(body -> "ok".equalsIgnoreCase(String.valueOf(body.get("status"))))
            .orElse(false);
    }

    public Optional<Map<String, Object>> chat(Map<String, Object> request) {
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

    public boolean chatStream(Map<String, Object> request, Consumer<String> chunkConsumer) {
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

    public Optional<List<Map<String, Object>>> extractMemories(Map<String, Object> request) {
        if (!isMemoryExtractionEnabled()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = webClient.post()
                .uri("/ai/memory/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            if (body == null) {
                return Optional.of(Collections.emptyList());
            }
            Object memoriesObj = body.get("memories");
            if (!(memoriesObj instanceof List<?> list)) {
                return Optional.of(Collections.emptyList());
            }
            List<Map<String, Object>> memories = objectMapper.convertValue(list, new TypeReference<>() {});
            return Optional.of(memories);
        } catch (Exception ex) {
            log.warn("AI memory extraction call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> synthesizeProfile(Map<String, Object> request) {
        if (!isProfileSynthesisEnabled()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = webClient.post()
                .uri("/ai/profile/synthesize")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI profile synthesis failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<List<Map<String, Object>>> queryRag(Map<String, Object> request) {
        if (!isRagEnabled()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = webClient.post()
                .uri("/ai/rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            if (body == null) {
                return Optional.of(Collections.emptyList());
            }
            Object chunksObj = body.get("chunks");
            if (!(chunksObj instanceof List<?>)) {
                return Optional.of(Collections.emptyList());
            }
            List<Map<String, Object>> chunks = objectMapper.convertValue(chunksObj, new TypeReference<>() {});
            return Optional.of(chunks);
        } catch (Exception ex) {
            log.warn("AI RAG query failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> indexRag(Map<String, Object> request) {
        if (!isRagEnabled()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = webClient.post()
                .uri("/ai/rag/index")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofMillis(properties.getReadTimeoutMs()));
            return Optional.ofNullable(body);
        } catch (Exception ex) {
            log.warn("AI RAG index failed: {}", ex.getMessage());
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