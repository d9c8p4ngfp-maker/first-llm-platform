package com.first.gateway.service.relay;

import com.first.gateway.domain.entity.*;
import com.first.gateway.repository.*;
import com.first.gateway.service.pipeline.PipelineConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatPipelineEnhancer {

    private static final Logger log = LoggerFactory.getLogger(ChatPipelineEnhancer.class);

    private final SkillRepository skillRepository;
    private final SkillBindingRepository skillBindingRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptVersionRepository promptVersionRepository;
    private final UserProfileRepository userProfileRepository;
    private final PipelineConfigService pipelineConfigService;

    public ChatPipelineEnhancer(SkillRepository skillRepository,
                                 SkillBindingRepository skillBindingRepository,
                                 PromptTemplateRepository promptTemplateRepository,
                                 PromptVersionRepository promptVersionRepository,
                                 UserProfileRepository userProfileRepository,
                                 PipelineConfigService pipelineConfigService) {
        this.skillRepository = skillRepository;
        this.skillBindingRepository = skillBindingRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.promptVersionRepository = promptVersionRepository;
        this.userProfileRepository = userProfileRepository;
        this.pipelineConfigService = pipelineConfigService;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> enhance(Map<String, Object> request, Long userId, Long tenantId) {
        Map<String, Object> enhanced = new HashMap<>(request);

        Long skillId = toLong(enhanced.remove("x_skill_id"));
        enhanced.remove("x_knowledge_base_ids");
        Long promptTemplateId = toLong(enhanced.remove("x_prompt_template_id"));
        Map<String, Object> promptVars = (Map<String, Object>) enhanced.remove("x_prompt_variables");

        StringBuilder systemPromptBuilder = new StringBuilder();
        String modelOverride = null;

        if (skillId != null) {
            try {
                Skill skill = skillRepository.findById(skillId).orElse(null);
                if (skill != null && skill.getDeleted() == 0) {
                    if (skill.getSuggestedModel() != null && !skill.getSuggestedModel().isBlank()) {
                        modelOverride = skill.getSuggestedModel();
                    }
                    if (skill.getPromptTemplateId() != null) {
                        appendPromptTemplate(systemPromptBuilder, skill.getPromptTemplateId(), promptVars);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load skill {}: {}", skillId, e.getMessage());
            }
        }

        if (promptTemplateId != null) {
            appendPromptTemplate(systemPromptBuilder, promptTemplateId, promptVars);
        }

        try {
            userProfileRepository.findByUserId(userId).ifPresent(profile -> {
                if (profile.getAiSystemPrompt() != null && !profile.getAiSystemPrompt().isBlank()) {
                    systemPromptBuilder.append("\n\n--- \u7528\u6237\u753b\u50cf ---\n").append(profile.getAiSystemPrompt());
                }
            });
        } catch (Exception e) {
            log.warn("Failed to load user profile for persona injection: {}", e.getMessage());
        }

        try {
            PipelineConfig baseConfig = pipelineConfigService.getByKey("chat.system_prompt_base", userId);
            if (baseConfig.getPromptText() != null && !baseConfig.getPromptText().isBlank()) {
                systemPromptBuilder.insert(0, baseConfig.getPromptText() + "\n\n");
            }
        } catch (Exception ignored) {
        }

        if (!systemPromptBuilder.isEmpty()) {
            injectSystemPrompt(enhanced, systemPromptBuilder.toString());
        }

        if (modelOverride != null && !enhanced.containsKey("model")) {
            enhanced.put("model", modelOverride);
        }

        return enhanced;
    }

    private void appendPromptTemplate(StringBuilder sb, Long templateId, Map<String, Object> vars) {
        promptTemplateRepository.findById(templateId).ifPresent(template -> {
            if (template.getDeleted() != 0) return;
            promptVersionRepository.findFirstByTemplateIdOrderByCreatedAtDesc(templateId).ifPresent(version -> {
                if (version.getSystemPrompt() != null && !version.getSystemPrompt().isBlank()) {
                    String rendered = version.getSystemPrompt();
                    if (vars != null) {
                        for (var entry : vars.entrySet()) {
                            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
                        }
                    }
                    sb.append(rendered);
                }
            });
        });
    }

    @SuppressWarnings("unchecked")
    private void injectSystemPrompt(Map<String, Object> request, String systemPrompt) {
        Object messages = request.get("messages");
        if (messages instanceof List<?> list) {
            List<Map<String, Object>> msgList = new ArrayList<>();
            boolean hasSystem = false;
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> msg = new HashMap<>((Map<String, Object>) map);
                    if ("system".equals(msg.get("role"))) {
                        String existing = msg.get("content") != null ? msg.get("content").toString() : "";
                        msg.put("content", existing + "\n\n" + systemPrompt);
                        hasSystem = true;
                    }
                    msgList.add(msg);
                }
            }
            if (!hasSystem) {
                Map<String, Object> sysMsg = new LinkedHashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                msgList.add(0, sysMsg);
            }
            request.put("messages", msgList);
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }
}
