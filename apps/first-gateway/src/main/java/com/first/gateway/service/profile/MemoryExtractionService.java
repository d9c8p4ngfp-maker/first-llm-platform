package com.first.gateway.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.PipelineConfig;
import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.enums.MemoryCategory;
import com.first.gateway.domain.enums.MemoryStatus;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.ai.dto.ChatMessage;
import com.first.gateway.infra.ai.dto.ExistingMemoryRef;
import com.first.gateway.infra.ai.dto.ExtractedMemory;
import com.first.gateway.infra.ai.dto.MemoryExtractRequest;
import com.first.gateway.infra.ai.dto.MemoryExtractResponse;
import com.first.gateway.infra.ai.dto.ModelConfig;
import com.first.gateway.infra.ai.dto.ModelParams;
import com.first.gateway.infra.ai.dto.UpstreamConfig;
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
import java.util.List;
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

            String promptText = config.getPromptText();

            List<ExtractedMemory> items = callExtraction(config, userId, existing, userMessage,
                assistantMessage, promptText);

            if (items == null || items.isEmpty()) {
                log.info("No memories extracted for user {}", userId);
                return;
            }

            int created = 0;
            int skippedDuplicate = 0;
            for (ExtractedMemory item : items) {
                try {
                    String categoryStr = item.category() != null ? item.category() : "FACT";
                    String content = item.content();
                    if (content == null || content.isBlank()) continue;

                    if (isDuplicate(content, existing)) {
                        skippedDuplicate++;
                        continue;
                    }

                    MemoryCategory category;
                    try {
                        category = MemoryCategory.valueOf(categoryStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        category = MemoryCategory.FACT;
                    }

                    UserMemory memory = new UserMemory();
                    memory.setUserId(userId);
                    memory.setTenantId(tenantId);
                    memory.setConversationId(conversationId);
                    memory.setSource("AI_EXTRACT");
                    memory.setCategory(category);
                    memory.setContent(content);
                    memory.setImportance(item.importance() != null ? item.importance().shortValue() : (short) 3);

                    if (item.scheduleDate() != null && !item.scheduleDate().isBlank() && !"null".equals(item.scheduleDate())) {
                        try {
                            memory.setScheduleDate(LocalDate.parse(item.scheduleDate()));
                        } catch (DateTimeParseException ignored) {}
                    }
                    memory.setScheduleTime(item.scheduleTime());

                    if (item.numericValue() != null) {
                        memory.setNumericValue(BigDecimal.valueOf(item.numericValue()));
                    }
                    memory.setStatus(MemoryStatus.ACTIVE);
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

    private List<ExtractedMemory> callExtraction(PipelineConfig config, Long userId,
                                                      List<UserMemory> existing,
                                                      String userMessage, String assistantMessage,
                                                      String promptText) {
        if (aiServiceProperties.isMemoryExtraction() && aiServiceClient.isHealthy()) {
            Optional<MemoryExtractResponse> viaPython = callExtractionViaPython(
                config, userId, existing, userMessage, assistantMessage, promptText);
            if (viaPython.isPresent() && viaPython.get().memories() != null) {
                log.info("Memory extraction via first-ai-service for user {}", userId);
                return viaPython.get().memories();
            }
        }
        log.error("Memory extraction unavailable for user {}", userId);
        return Collections.emptyList();
    }

    private Optional<MemoryExtractResponse> callExtractionViaPython(PipelineConfig config, Long userId,
                                                                      List<UserMemory> existing,
                                                                      String userMessage,
                                                                      String assistantMessage,
                                                                      String promptText) {
        try {
            String model = config.getModelId() != null && !config.getModelId().isBlank()
                ? config.getModelId() : "deepseek-chat";
            Channel channel = channelSelector.selectForModel(model, userId);

            MemoryExtractRequest req = new MemoryExtractRequest(
                List.of(
                    new ChatMessage("user", userMessage),
                    new ChatMessage("assistant", assistantMessage)
                ),
                formatExistingForPython(existing),
                new ModelConfig(model, parseModelParams(config.getModelParams()), promptText),
                new UpstreamConfig(
                    channel.getBaseUrl(),
                    channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()),
                    model));

            return aiServiceClient.extractMemories(req);
        } catch (RuntimeException e) {
            log.warn("callExtractionViaPython failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<ExistingMemoryRef> formatExistingForPython(List<UserMemory> memories) {
        List<ExistingMemoryRef> list = new ArrayList<>();
        int limit = Math.min(memories.size(), MAX_EXISTING_MEMORIES);
        for (int i = 0; i < limit; i++) {
            UserMemory m = memories.get(i);
            list.add(new ExistingMemoryRef(
                m.getId(),
                m.getCategory() != null ? m.getCategory().name() : "",
                m.getContent()));
        }
        return list;
    }

    private ModelParams parseModelParams(String modelParamsJson) {
        if (modelParamsJson == null || modelParamsJson.isBlank()) {
            return new ModelParams(DEFAULT_MEMORY_TEMPERATURE, DEFAULT_MEMORY_MAX_TOKENS);
        }
        try {
            return objectMapper.readValue(modelParamsJson, ModelParams.class);
        } catch (IOException e) {
            return new ModelParams(DEFAULT_MEMORY_TEMPERATURE, DEFAULT_MEMORY_MAX_TOKENS);
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
