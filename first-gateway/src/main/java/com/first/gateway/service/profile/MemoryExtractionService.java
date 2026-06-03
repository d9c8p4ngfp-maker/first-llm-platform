package com.first.gateway.service.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.PipelineConfig;
import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.relay.adapter.OpenAiAdapter;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.service.pipeline.PipelineConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MemoryExtractionService {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractionService.class);

    private static final Pattern JSON_FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final UserMemoryService memoryService;
    private final UserProfileService profileService;
    private final PipelineConfigService pipelineConfigService;
    private final ProfileSynthesisService synthesisService;
    private final ChannelSelector channelSelector;
    private final OpenAiAdapter openAiAdapter;
    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;
    private final ChannelKeyCrypto channelKeyCrypto;
    private final ObjectMapper objectMapper;

    public MemoryExtractionService(UserMemoryService memoryService,
                                    UserProfileService profileService,
                                    PipelineConfigService pipelineConfigService,
                                    ProfileSynthesisService synthesisService,
                                    ChannelSelector channelSelector,
                                    OpenAiAdapter openAiAdapter,
                                    AiServiceClient aiServiceClient,
                                    AiServiceProperties aiServiceProperties,
                                    ChannelKeyCrypto channelKeyCrypto,
                                    ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.profileService = profileService;
        this.pipelineConfigService = pipelineConfigService;
        this.synthesisService = synthesisService;
        this.channelSelector = channelSelector;
        this.openAiAdapter = openAiAdapter;
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
            } catch (Exception e) {
                log.debug("No memory.extraction pipeline config found, skipping");
                return;
            }

            if (config.getEnabled() != null && config.getEnabled() == 0) {
                log.debug("Memory extraction disabled for user {}", userId);
                return;
            }

            List<UserMemory> existing = memoryService.listForUser(userId, null);
            String existingSummary = formatExistingSummary(existing);

            String conversationSegment = "## User message (primary source):\n" + userMessage
                + "\n\n## Assistant reply (context only, do not extract assistant jokes):\n" + assistantMessage;

            String promptText = getDefaultExtractionPrompt(LocalDate.now());

            List<Map<String, Object>> items = callExtraction(config, userId, existing, userMessage,
                assistantMessage, existingSummary, conversationSegment, promptText);

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
                        } catch (Exception ignored) {}
                    }
                    memory.setScheduleTime((String) item.get("schedule_time"));

                    Object numVal = item.get("numeric_value");
                    if (numVal instanceof Number) {
                        memory.setNumericValue(BigDecimal.valueOf(((Number) numVal).doubleValue()));
                    }
                    memory.setStatus("ACTIVE");
                    memoryService.createFromExtraction(memory);
                    created++;
                } catch (Exception e) {
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
        } catch (Exception e) {
            log.error("Memory extraction failed for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> callExtraction(PipelineConfig config, Long userId,
                                                      List<UserMemory> existing,
                                                      String userMessage, String assistantMessage,
                                                      String existingSummary,
                                                      String conversationSegment,
                                                      String promptText) {
        if (aiServiceProperties.isMemoryExtraction() && aiServiceClient.isHealthy()) {
            Optional<List<Map<String, Object>>> viaPython = callExtractionViaPython(
                config, userId, existing, userMessage, assistantMessage, promptText);
            if (viaPython.isPresent()) {
                log.info("Memory extraction via first-ai-service for user {}", userId);
                return viaPython.get();
            }
            log.warn("Python memory extraction unavailable, falling back to Java for user {}", userId);
        }
        String llmResponse = callExtractionModelJava(config, userId, existingSummary, conversationSegment, promptText);
        if (llmResponse == null || llmResponse.isBlank()) {
            return Collections.emptyList();
        }
        return parseExtractionResult(llmResponse);
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
        } catch (Exception e) {
            log.warn("callExtractionViaPython failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<Map<String, Object>> formatExistingForPython(List<UserMemory> memories) {
        List<Map<String, Object>> list = new ArrayList<>();
        int limit = Math.min(memories.size(), 30);
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
            return Map.of("temperature", 0.2, "max_tokens", 2000);
        }
        try {
            return objectMapper.readValue(modelParamsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("temperature", 0.2, "max_tokens", 2000);
        }
    }

    private String callExtractionModelJava(PipelineConfig config, Long userId, String existingSummary,
                                        String conversationSegment, String promptText) {
        String model = config.getModelId() != null && !config.getModelId().isBlank()
            ? config.getModelId() : "deepseek-chat";
        String systemPrompt = promptText != null && !promptText.isBlank()
            ? promptText : getDefaultExtractionPrompt(LocalDate.now());
        String userContent = "## Existing memories:\n" + existingSummary
            + "\n\n## Conversation:\n" + conversationSegment;
        try {
            Channel channel = channelSelector.selectForModel(model, userId);
            Map<String, Object> response = openAiAdapter.chat(channel, Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userContent)
                ),
                "temperature", 0.2,
                "max_tokens", 2000
            ));
            return extractAssistantContent(response);
        } catch (Exception e) {
            log.warn("Memory extraction LLM call failed for user {}: {}", userId, e.getMessage());
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractAssistantContent(Map<String, Object> response) {
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return "[]";
        }
        Object first = choices.getFirst();
        if (!(first instanceof Map<?, ?> choice)) {
            return "[]";
        }
        Object messageObj = choice.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return "[]";
        }
        Object content = message.get("content");
        return content != null ? content.toString().trim() : "[]";
    }

    private List<Map<String, Object>> parseExtractionResult(String json) {
        String normalized = normalizeJsonPayload(json);
        try {
            return objectMapper.readValue(normalized, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse extraction JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String normalizeJsonPayload(String json) {
        if (json == null || json.isBlank()) {
            return "[]";
        }
        String trimmed = json.trim();
        Matcher matcher = JSON_FENCE.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
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

    private String formatExistingSummary(List<UserMemory> memories) {
        if (memories.isEmpty()) return "No existing memories";
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(memories.size(), 30);
        for (int i = 0; i < limit; i++) {
            UserMemory m = memories.get(i);
            sb.append("- [").append(m.getCategory()).append("]");
            if (m.getScheduleDate() != null) {
                sb.append(" date=").append(m.getScheduleDate());
            }
            if (m.getScheduleTime() != null) {
                sb.append(" time=").append(m.getScheduleTime());
            }
            sb.append(" ").append(m.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String getDefaultExtractionPrompt(LocalDate today) {
        return "You are an information extraction expert. Extract structured memories from user conversations.\n"
            + "Today's date is " + today + ". Resolve relative dates (today/tomorrow/\u540e\u5929/\u5468\u672b) against this date.\n"
            + "Rules:\n"
            + "1. Prioritize the USER message; ignore assistant humor, emojis, and generic advice\n"
            + "2. Extract TODO or SCHEDULE for near-term plans even without exact clock time "
            + "(e.g. \u4e00\u4f1a\u513f/\u5f85\u4f1a/\u4eca\u5929\u60f3\u5403 -> category TODO or SCHEDULE, schedule_date=today, schedule_time null)\n"
            + "3. Extract PREFERENCE when user mentions food/taste/habit preferences\n"
            + "4. Dedup only when same date AND same time AND same event as an existing memory; "
            + "today eating is NOT duplicate of a different day's meal\n"
            + "5. Skip pure greetings with no factual content\n"
            + "Output format (strict JSON array only):\n"
            + "[{\"category\":\"FACT|PREFERENCE|EVENT|GOAL|TODO|SCHEDULE\",\"content\":\"...\","
            + "\"importance\":1-5,\"schedule_date\":\"YYYY-MM-DD or null\","
            + "\"schedule_time\":\"HH:mm or null\",\"numeric_value\":null}]\n"
            + "Return [] only when the user message contains no extractable fact, plan, or preference.";
    }
}
