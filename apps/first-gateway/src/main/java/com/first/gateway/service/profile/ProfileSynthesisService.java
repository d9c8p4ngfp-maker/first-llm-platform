package com.first.gateway.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.PipelineConfig;
import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.entity.UserProfile;
import com.first.gateway.domain.enums.SynthesisStatus;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.ai.dto.AiPersonality;
import com.first.gateway.infra.ai.dto.CurrentProfile;
import com.first.gateway.infra.ai.dto.MemoryItem;
import com.first.gateway.infra.ai.dto.ModelParams;
import com.first.gateway.infra.ai.dto.ProfileSynthesizeRequest;
import com.first.gateway.infra.ai.dto.ProfileSynthesizeResponse;
import com.first.gateway.infra.ai.dto.SynthesisConfig;
import com.first.gateway.infra.ai.dto.UpstreamConfig;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.repository.AsyncTaskRepository;
import com.first.gateway.repository.UserProfileRepository;
import com.first.gateway.service.pipeline.PipelineConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
    private final AsyncTaskRepository asyncTaskRepository;

    public ProfileSynthesisService(UserProfileRepository profileRepository,
                                    UserMemoryService memoryService,
                                    PipelineConfigService pipelineConfigService,
                                    AiServiceClient aiServiceClient,
                                    AiServiceProperties aiServiceProperties,
                                    ChannelSelector channelSelector,
                                    ChannelKeyCrypto channelKeyCrypto,
                                    ObjectMapper objectMapper,
                                    AsyncTaskRepository asyncTaskRepository) {
        this.profileRepository = profileRepository;
        this.memoryService = memoryService;
        this.pipelineConfigService = pipelineConfigService;
        this.aiServiceClient = aiServiceClient;
        this.aiServiceProperties = aiServiceProperties;
        this.channelSelector = channelSelector;
        this.channelKeyCrypto = channelKeyCrypto;
        this.objectMapper = objectMapper;
        this.asyncTaskRepository = asyncTaskRepository;
    }

    public void checkAndTrigger(Long userId, Long tenantId) {
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return;

        if (profile.getSynthesisStatus() == SynthesisStatus.PENDING
            || profile.getSynthesisStatus() == SynthesisStatus.RUNNING) {
            return;
        }

        int currentCount = profile.getMemoryCount() != null ? profile.getMemoryCount() : 0;
        int lastSynthesisCount = profile.getLastSynthesisCount() != null ? profile.getLastSynthesisCount() : 0;
        int newMemories = currentCount - lastSynthesisCount;

        if (newMemories >= 5) {
            profile.setSynthesisStatus(SynthesisStatus.PENDING);
            profileRepository.save(profile);

            AsyncTask task = new AsyncTask();
            task.setTaskType("PROFILE_SYNTHESIS");
            task.setRefId(profile.getId());
            asyncTaskRepository.save(task);
        }
    }

    @Transactional
    public void doSynthesis(UserProfile profile) {
        PipelineConfig config;
        try {
            config = pipelineConfigService.getByKey("memory.synthesis", profile.getUserId());
        } catch (Exception e) {
            log.debug("No memory.synthesis pipeline config, using defaults");
            config = null;
        }

        List<UserMemory> activeMemories = memoryService.listForUser(profile.getUserId(), null);
        int limit = Math.min(activeMemories.size(), 50);
        activeMemories = activeMemories.subList(0, limit);

        boolean updated = applySynthesis(profile.getUserId(), profile, activeMemories, config);

        profile.setLastSynthesisCount(profile.getMemoryCount());
        profile.setVersion((profile.getVersion() != null ? profile.getVersion() : 0) + 1);
        profileRepository.save(profile);

        log.info("Profile synthesis completed for user {} (updated={})", profile.getUserId(), updated);
    }

    private boolean applySynthesis(Long userId, UserProfile profile, List<UserMemory> memories, PipelineConfig config) {
        if (aiServiceProperties.isProfileSynthesis() && aiServiceClient.isHealthy()) {
            Optional<ProfileSynthesizeResponse> result = callPythonSynthesis(userId, profile, memories, config);
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

    private Optional<ProfileSynthesizeResponse> callPythonSynthesis(Long userId, UserProfile profile,
                                                                       List<UserMemory> memories, PipelineConfig config) {
        try {
            String model = config != null && config.getModelId() != null && !config.getModelId().isBlank()
                ? config.getModelId() : "deepseek-chat";
            Channel channel = channelSelector.selectForModel(model, userId);

            List<MemoryItem> memItems = new ArrayList<>();
            for (UserMemory m : memories) {
                memItems.add(new MemoryItem(
                    m.getCategory() != null ? m.getCategory().name() : "",
                    m.getContent(),
                    m.getImportance() != null ? m.getImportance().intValue() : 3));
            }

            AiPersonality personality = new AiPersonality(
                profile.getMbti(), profile.getMbtiLabel(), profile.getZodiac());

            CurrentProfile current = new CurrentProfile(
                profile.getAiSummary(),
                profile.getAiTags(),
                personality);

            SynthesisConfig synthCfg = new SynthesisConfig(
                model,
                config != null ? parseModelParams(config.getModelParams()) : new ModelParams(0.3, 3000),
                config != null ? config.getPromptText() : null);

            UpstreamConfig upstream = new UpstreamConfig(
                channel.getBaseUrl(),
                channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()),
                model);

            ProfileSynthesizeRequest req = new ProfileSynthesizeRequest(
                memItems, current, synthCfg, upstream);

            return aiServiceClient.synthesizeProfile(req);
        } catch (Exception e) {
            log.warn("callPythonSynthesis failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void applyProfileResult(UserProfile profile, ProfileSynthesizeResponse response) {
        if (response.profile() == null) {
            return;
        }
        var pr = response.profile();
        if (pr.aiSummary() != null) {
            profile.setAiSummary(pr.aiSummary());
        }
        if (pr.aiSystemPrompt() != null) {
            profile.setAiSystemPrompt(pr.aiSystemPrompt());
        }
        if (pr.aiTags() != null) {
            profile.setAiTags(pr.aiTags());
        }
        if (pr.aiPersonality() != null) {
            var pers = pr.aiPersonality();
            if (pers.mbti() != null) profile.setMbti(pers.mbti());
            if (pers.mbtiLabel() != null) profile.setMbtiLabel(pers.mbtiLabel());
            if (pers.zodiac() != null) profile.setZodiac(pers.zodiac());
        }
    }

    private ModelParams parseModelParams(String json) {
        try {
            return objectMapper.readValue(json, ModelParams.class);
        } catch (Exception e) {
            return new ModelParams(0.3, 3000);
        }
    }
}
