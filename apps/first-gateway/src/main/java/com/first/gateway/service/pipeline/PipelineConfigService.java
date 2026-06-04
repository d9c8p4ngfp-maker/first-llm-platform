package com.first.gateway.service.pipeline;

import com.first.gateway.domain.entity.PipelineConfig;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.PipelineConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class PipelineConfigService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}");

    private final PipelineConfigRepository repository;

    public PipelineConfigService(PipelineConfigRepository repository) {
        this.repository = repository;
    }

    public List<PipelineConfig> listForUser(Long userId) {
        List<PipelineConfig> systemRows = repository.findByScopeOrUserId("SYSTEM", userId).stream()
            .filter(c -> "SYSTEM".equals(c.getScope()))
            .toList();
        List<PipelineConfig> userRows = repository.findByScopeOrUserId("USER", userId).stream()
            .filter(c -> "USER".equals(c.getScope()) && userId.equals(c.getUserId()))
            .toList();

        Map<String, PipelineConfig> merged = new LinkedHashMap<>();
        for (PipelineConfig config : systemRows) {
            merged.put(config.getConfigKey(), config);
        }
        for (PipelineConfig config : userRows) {
            merged.put(config.getConfigKey(), config);
        }
        return new ArrayList<>(merged.values());
    }

    public PipelineConfig getByKey(String configKey, Long userId) {
        return repository.findByConfigKeyAndUserId(configKey, userId)
            .filter(c -> "USER".equals(c.getScope()))
            .or(() -> repository.findByConfigKeyAndScopeAndUserId(configKey, "SYSTEM", 0L))
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "pipeline config not found"));
    }

    @Transactional
    public PipelineConfig saveOverride(Long userId, String configKey, String modelId, String modelParams,
                                       Long promptTemplateId, String promptText, Short enabled,
                                       String description) {
        PipelineConfig config = repository.findByConfigKeyAndScopeAndUserId(configKey, "USER", userId)
            .orElseGet(() -> {
                PipelineConfig created = new PipelineConfig();
                created.setConfigKey(configKey);
                created.setScope("USER");
                created.setUserId(userId);
                return created;
            });
        if (modelId != null) {
            config.setModelId(modelId);
        }
        if (modelParams != null) {
            config.setModelParams(modelParams);
        }
        if (promptTemplateId != null) {
            config.setPromptTemplateId(promptTemplateId);
        }
        if (promptText != null) {
            config.setPromptText(promptText);
        }
        if (enabled != null) {
            config.setEnabled(enabled);
        }
        if (description != null) {
            config.setDescription(description);
        }
        return repository.save(config);
    }

    @Transactional
    public void deleteOverride(String configKey, Long userId) {
        repository.findByConfigKeyAndScopeAndUserId(configKey, "USER", userId)
            .ifPresent(repository::delete);
    }

    public Map<String, Object> preview(String configKey, Long userId, Map<String, String> variables) {
        PipelineConfig config = getByKey(configKey, userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("config_key", configKey);
        body.put("model_id", config.getModelId());
        body.put("prompt_text", render(config.getPromptText(), variables));
        return body;
    }

    private static String render(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}