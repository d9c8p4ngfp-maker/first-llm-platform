package com.first.gateway.service.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.PipelineConfig;
import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.entity.UserProfile;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.repository.UserProfileRepository;
import com.first.gateway.service.pipeline.PipelineConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProfileSynthesisService {

    private static final Logger log = LoggerFactory.getLogger(ProfileSynthesisService.class);

    private final UserProfileRepository profileRepository;
    private final UserMemoryService memoryService;
    private final PipelineConfigService pipelineConfigService;
    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;
    private final ChannelSelector channelSelector;
    private final ChannelKeyCrypto channelKeyCrypto;
    private final ObjectMapper objectMapper;

    public ProfileSynthesisService(UserProfileRepository profileRepository,
                                    UserMemoryService memoryService,
                                    PipelineConfigService pipelineConfigService,
                                    AiServiceClient aiServiceClient,
                                    AiServiceProperties aiServiceProperties,
                                    ChannelSelector channelSelector,
                                    ChannelKeyCrypto channelKeyCrypto,
                                    ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.memoryService = memoryService;
        this.pipelineConfigService = pipelineConfigService;
        this.aiServiceClient = aiServiceClient;
        this.aiServiceProperties = aiServiceProperties;
        this.channelSelector = channelSelector;
        this.channelKeyCrypto = channelKeyCrypto;
        this.objectMapper = objectMapper;
    }

    public void checkAndTrigger(Long userId, Long tenantId) {
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return;

        int currentCount = profile.getMemoryCount() != null ? profile.getMemoryCount() : 0;
        int lastSynthesisCount = profile.getLastSynthesisCount() != null ? profile.getLastSynthesisCount() : 0;
        int newMemories = currentCount - lastSynthesisCount;

        if (newMemories >= 5) {
            synthesize(userId, tenantId);
        }
    }

    @Async("profileSynthesisExecutor")
    @Transactional
    public void synthesize(Long userId, Long tenantId) {
        try {
            UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
            if (profile == null) return;

            if ("RUNNING".equals(profile.getSynthesisStatus())) {
                log.info("Profile synthesis already running for user {}", userId);
                return;
            }

            profile.setSynthesisStatus("RUNNING");
            profileRepository.save(profile);

            PipelineConfig config;
            try {
                config = pipelineConfigService.getByKey("memory.synthesis", userId);
            } catch (Exception e) {
                log.debug("No memory.synthesis pipeline config, using defaults");
                config = null;
            }

            List<UserMemory> activeMemories = memoryService.listForUser(userId, null);
            int limit = Math.min(activeMemories.size(), 50);
            activeMemories = activeMemories.subList(0, limit);

            boolean updated = applySynthesis(userId, profile, activeMemories, config);

            profile.setLastSynthesisCount(profile.getMemoryCount());
            profile.setSynthesisStatus(updated ? "IDLE" : "IDLE");
            profile.setVersion((profile.getVersion() != null ? profile.getVersion() : 0) + 1);
            profileRepository.save(profile);

            log.info("Profile synthesis completed for user {} (updated={})", userId, updated);
        } catch (Exception e) {
            log.error("Profile synthesis failed for user {}: {}", userId, e.getMessage(), e);
            profileRepository.findByUserId(userId).ifPresent(p -> {
                p.setSynthesisStatus("FAILED");
                profileRepository.save(p);
            });
        }
    }

    private boolean applySynthesis(Long userId, UserProfile profile, List<UserMemory> memories, PipelineConfig config) {
        if (aiServiceProperties.isProfileSynthesis() && aiServiceClient.isHealthy()) {
            Optional<Map<String, Object>> result = callPythonSynthesis(userId, profile, memories, config);
            if (result.isPresent()) {
                applyProfileResult(profile, result.get());
                log.info("Profile synthesis via first-ai-service for user {}", userId);
                return true;
            }
            log.warn("Python profile synthesis unavailable for user {}", userId);
        }
        log.info("Profile synthesis skipped (no Python) for user {} with {} memories", userId, memories.size());
        return false;
    }

    private Optional<Map<String, Object>> callPythonSynthesis(Long userId, UserProfile profile,
                                                               List<UserMemory> memories, PipelineConfig config) {
        try {
            String model = config != null && config.getModelId() != null && !config.getModelId().isBlank()
                ? config.getModelId() : "deepseek-chat";
            Channel channel = channelSelector.selectForModel(model, userId);

            List<Map<String, Object>> memRows = new ArrayList<>();
            for (UserMemory m : memories) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("category", m.getCategory());
                row.put("content", m.getContent());
                row.put("importance", m.getImportance() != null ? m.getImportance() : 3);
                memRows.add(row);
            }

            Map<String, Object> current = new LinkedHashMap<>();
            current.put("ai_summary", profile.getAiSummary());
            current.put("ai_tags", parseTags(profile.getAiTags()));
            Map<String, Object> personality = new LinkedHashMap<>();
            if (profile.getMbti() != null) personality.put("mbti", profile.getMbti());
            if (profile.getMbtiLabel() != null) personality.put("mbti_label", profile.getMbtiLabel());
            if (profile.getZodiac() != null) personality.put("zodiac", profile.getZodiac());
            current.put("ai_personality", personality);

            Map<String, Object> cfg = new LinkedHashMap<>();
            cfg.put("model", model);
            if (config != null && config.getModelParams() != null) {
                cfg.put("model_params", parseModelParams(config.getModelParams()));
            }
            if (config != null && config.getPromptText() != null) {
                cfg.put("prompt_override", config.getPromptText());
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("memories", memRows);
            body.put("current_profile", current);
            body.put("config", cfg);

            Map<String, Object> upstream = new LinkedHashMap<>();
            upstream.put("base_url", channel.getBaseUrl());
            upstream.put("api_key", channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()));
            upstream.put("model", model);
            body.put("upstream", upstream);

            return aiServiceClient.synthesizeProfile(body);
        } catch (Exception e) {
            log.warn("callPythonSynthesis failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private void applyProfileResult(UserProfile profile, Map<String, Object> response) {
        Object profileObj = response.get("profile");
        if (!(profileObj instanceof Map<?, ?> map)) {
            return;
        }
        Map<String, Object> p = objectMapper.convertValue(map, new TypeReference<>() {});
        if (p.get("ai_summary") != null) {
            profile.setAiSummary(p.get("ai_summary").toString());
        }
        if (p.get("ai_system_prompt") != null) {
            profile.setAiSystemPrompt(p.get("ai_system_prompt").toString());
        }
        if (p.get("ai_tags") instanceof List<?> tags) {
            try {
                profile.setAiTags(objectMapper.writeValueAsString(tags));
            } catch (Exception ignored) {
            }
        }
        if (p.get("ai_personality") instanceof Map<?, ?> pers) {
            Object mbti = pers.get("mbti");
            if (mbti != null) profile.setMbti(mbti.toString());
            Object label = pers.get("mbti_label");
            if (label != null) profile.setMbtiLabel(label.toString());
            Object zodiac = pers.get("zodiac");
            if (zodiac != null) profile.setZodiac(zodiac.toString());
        }
    }

    private List<String> parseTags(String aiTags) {
        if (aiTags == null || aiTags.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(aiTags, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of(aiTags.trim());
        }
    }

    private Map<String, Object> parseModelParams(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("temperature", 0.3, "max_tokens", 3000);
        }
    }
}