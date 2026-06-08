package com.first.gateway.service.profile;

import com.first.gateway.domain.entity.PipelineConfig;
import com.first.gateway.repository.PipelineConfigRepository;
import com.first.gateway.web.workspace.dto.ProfileStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProfileStatusService {

    private static final String MEMORY_EXTRACTION = "memory.extraction";
    private static final String MEMORY_SYNTHESIS = "memory.synthesis";
    private static final String CHAT_SYSTEM_PROMPT = "chat.system_prompt_base";

    private final PipelineConfigRepository repository;

    public ProfileStatusService(PipelineConfigRepository repository) {
        this.repository = repository;
    }

    public ProfileStatusResponse getStatus(Long userId) {
        return new ProfileStatusResponse(
            isEnabled(MEMORY_EXTRACTION, userId),
            isEnabled(MEMORY_SYNTHESIS, userId),
            isEnabled(CHAT_SYSTEM_PROMPT, userId));
    }

    @Transactional
    public ProfileStatusResponse updateStatus(Long userId, boolean memoryEnabled,
                                               boolean profileEnabled, boolean profileInChat) {
        saveOverride(MEMORY_EXTRACTION, userId, memoryEnabled);
        saveOverride(MEMORY_SYNTHESIS, userId, profileEnabled);
        saveOverride(CHAT_SYSTEM_PROMPT, userId, profileInChat);
        return getStatus(userId);
    }

    private boolean isEnabled(String configKey, Long userId) {
        return repository.findByConfigKeyAndUserId(configKey, userId)
            .filter(c -> "USER".equals(c.getScope()))
            .or(() -> repository.findByConfigKeyAndScopeAndUserId(configKey, "SYSTEM", 0L))
            .map(c -> c.getEnabled() != null && c.getEnabled() == 1)
            .orElse(false);
    }

    private void saveOverride(String configKey, Long userId, boolean enabled) {
        PipelineConfig config = repository
            .findByConfigKeyAndScopeAndUserId(configKey, "USER", userId)
            .orElseGet(() -> {
                PipelineConfig created = new PipelineConfig();
                created.setConfigKey(configKey);
                created.setScope("USER");
                created.setUserId(userId);
                return created;
            });
        config.setEnabled((short) (enabled ? 1 : 0));
        repository.save(config);
    }
}
