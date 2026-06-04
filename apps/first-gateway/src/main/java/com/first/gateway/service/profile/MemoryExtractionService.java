package com.first.gateway.service.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.PipelineConfig;
import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.service.pipeline.PipelineConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MemoryExtractionService {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractionService.class);

    private static final int MAX_EXISTING_MEMORIES = 30;
    private static final double DEFAULT_MEMORY_TEMPERATURE = 0.2;
    private static final int DEFAULT_MEMORY_MAX_TOKENS = 2000;

    private final UserMemoryService memoryService;
    private final UserProfileService profileService;
    private final PipelineConfigService pipelineConfigService;
    private final ProfileSynthesisService synthesisService;
    private final ChannelSelector channelSelector;
    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;
    private final ChannelKeyCrypto channelKeyCrypto;
    private final ObjectMapper objectMapper;

    public MemoryExtractionService(UserMemoryService memoryService,
                                    UserProfileService profileService,
                                    PipelineConfigService pipelineConfigService,
                                    ProfileSynthesisService synthesisService,
                                    ChannelSelector channelSelector,
                                    AiServiceClient aiServiceClient,
                                    AiServiceProperties aiServiceProperties,
                                    ChannelKeyCrypto channelKeyCrypto,
                                    ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.profileService = profileService;
        this.pipelineConfigService = pipelineConfigService;
        this.synthesisService = synthesisService;
        this.channelSelector = channelSelector;
        this.aiServiceClient = aiServiceClient;
        this.aiServiceProperties = aiServiceProperties;
        this.channelKeyCrypto = channelKeyCrypto;
        this.objectMapper = objectMapper;
    }

    @Async("memoryExtractExecutor")
    public void extractAfterChat(Long userId, Long tenantId, Long conversationId,
                                  String userMessage, String assistantMessage) {
        try {
            log.info("Starting memory extraction for user {} conversation {}", userId, conversationId);

            PipelineConfig config;
            try {
                config = pipelineConfigService.getByKey("memory.extraction", userId);
            } catch (RuntimeException e) {
                log.debug("No memory.extraction pipeline config found, skipping");
                return;
            }

            if (config.getEnabled() != null && config.getEnabled() == 0) {
                log.debug("Memory extraction disabled for user {}", userId);
                return;
            }

            List<UserMemory> existing = memoryService.listForUser(userId, null);

            String promptText = pipelineConfigService.getPromptText("memory.extraction", userId);

            List<Map<String, Object>> items = callExtraction(config, userId, existing, userMessage,
                assistantMessage, promptText);

            if (items == null || items.isEmpty()) {
                log.info("No memories extracted for user {}", userId);
                return;
            }

            int created = 0;
            int skippedDuplicate = 0;
            for (Map<String, Object> item : items) {
                try {
                    String category = (String) item.getOrDefault("category", "FACT");
                    String content = (String) item.get("content");
                    if (content == null || content.isBlank()) continue;

                    if (isDuplicate(content, existing)) {
                        skippedDuplicate++;
                        continue;
                    }

                    UserMemory memory = new UserMemory();
                    memory.setUserId(userId);
                    memory.setTenantId(tenantId);
                    memory.setConversationId(conversationId);
                    memory.setSource("AI_EXTRACT");
                    memory.setCategory(category);
                    memory.setContent(content);
                    Object importance = item.get("importance");
                    memory.setImportance(importance instanceof Number ? ((Number) importance).shortValue() : (short) 3);

                    String schedDate = (String) item.get("schedule_date");
                    if (schedDate != null && !schedDate.isBlank() && !"null".equals(schedDate)) {
                        try {
                            memory.setScheduleDate(LocalDate.parse(schedDate));
                        } catch (DateTimeParseException ignored) {}
                    memory.setScheduleTime((String) item.get("schedule_time"));

                    Object numVal = item.get("numeric_value");
                    if (numVal instanceof Number) {
                        memory.setNumericValue(BigDecimal.valueOf(((Number) numVal).doubleValue()));
                    }
                    memory.setStatus("ACTIVE");
                    memoryService.createFromExtraction(memory);
                    created++;
                } catch (RuntimeException e) {
                    log.warn("Failed to process extraction item: {}", e.getMessage());
                }
            }

            if (created > 0) {
                log.info("Extracted {} memories for user {}", created, userId);
                profileService.updateMemoryCount(userId, tenantId);
                synthesisService.checkAndTrigger(userId, tenantId);
            } else {
                log.info("No memories saved for user {} (parsed {} items, skipped {} duplicates)",
                    userId, items.size(), skippedDuplicate);
            }
        } catch (RuntimeException e) {
            log.error("Memory extraction failed for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> callExtraction(PipelineConfig config, Long userId,
                                                      List<UserMemory> existing,
                                                      String userMessage, String assistantMessage,
                                                      String promptText) {
        if (aiServiceProperties.isMemoryExtraction() && aiServiceClient.isHealthy()) {
            Optional<List<Map<String, Object>>> viaPython = callExtractionViaPython(
                config, userId, existing, userMessage, assistantMessage, promptText);
            if (viaPython.isPresent()) {
                log.info("Memory extraction via first-ai-service for user {}", userId);
                return viaPython.get();
            }
        }
        log.error("Memory extraction unavailable for user {}", userId);
        return Collections.emptyList();
    }

    private Optional<List<Map<String, Object>>> callExtractionViaPython(PipelineConfig config, Long userId,
                                                                          List<UserMemory> existing,
                                                                          String userMessage,
                                                                          String assistantMessage,
                                                                          String promptText) {
        try {
            String model = config.getModelId() != null && !config.getModelId().isBlank()
                ? config.getModelId() : "deepseek-chat";
            Channel channel = channelSelector.selectForModel(model, userId);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("conversation_segment", List.of(
                Map.of("role", "user", "content", userMessage),
                Map.of("role", "assistant", "content", assistantMessage)
            ));
            body.put("existing_memories", formatExistingForPython(existing));

            Map<String, Object> cfg = new LinkedHashMap<>();
            cfg.put("model", model);
            cfg.put("model_params", parseModelParams(config.getModelParams()));
            cfg.put("prompt_override", promptText);
            body.put("config", cfg);

            Map<String, Object> upstream = new LinkedHashMap<>();
            upstream.put("base_url", channel.getBaseUrl());
            upstream.put("api_key", channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()));
            upstream.put("model", model);
            body.put("upstream", upstream);

            return aiServiceClient.extractMemories(body);
        } catch (RuntimeException e) {
            log.warn("callExtractionViaPython failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<Map<String, Object>> formatExistingForPython(List<UserMemory> memories) {
        List<Map<String, Object>> list = new ArrayList<>();
        int limit = Math.min(memories.size(), MAX_EXISTING_MEMORIES);
        for (int i = 0; i < limit; i++) {
            UserMemory m = memories.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("category", m.getCategory());
            row.put("content", m.getContent());
            list.add(row);
        }
        return list;
    }

    private Map<String, Object> parseModelParams(String modelParamsJson) {
        if (modelParamsJson == null || modelParamsJson.isBlank()) {
            return Map.of("temperature", DEFAULT_MEMORY_TEMPERATURE, "max_tokens", DEFAULT_MEMORY_MAX_TOKENS);
        }
        try {
            return objectMapper.readValue(modelParamsJson, new TypeReference<>() {});
        } catch (IOException e) {
            return Map.of("temperature", DEFAULT_MEMORY_TEMPERATURE, "max_tokens", DEFAULT_MEMORY_MAX_TOKENS);
        }
    }

    private boolean isDuplicate(String content, List<UserMemory> existing) {
        String lower = content.toLowerCase().trim();
        for (UserMemory m : existing) {
            if (m.getContent() != null && m.getContent().toLowerCase().trim().equals(lower)) {
                return true;
            }
        }
        return false;
    }
}
