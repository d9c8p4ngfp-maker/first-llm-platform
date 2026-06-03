package com.first.gateway.service.relay;

import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.*;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.repository.*;
import com.first.gateway.service.knowledge.KnowledgeBaseService;
import com.first.gateway.service.pipeline.PipelineConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatPipelineEnhancer {

    private static final Logger log = LoggerFactory.getLogger(ChatPipelineEnhancer.class);

    private final SkillRepository skillRepository;
    private final SkillBindingRepository skillBindingRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptVersionRepository promptVersionRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final UserProfileRepository userProfileRepository;
    private final PipelineConfigService pipelineConfigService;
    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;
    private final ChannelSelector channelSelector;
    private final ChannelKeyCrypto channelKeyCrypto;

    public ChatPipelineEnhancer(SkillRepository skillRepository,
                                 SkillBindingRepository skillBindingRepository,
                                 PromptTemplateRepository promptTemplateRepository,
                                 PromptVersionRepository promptVersionRepository,
                                 KnowledgeBaseService knowledgeBaseService,
                                 UserProfileRepository userProfileRepository,
                                 PipelineConfigService pipelineConfigService,
                                 AiServiceClient aiServiceClient,
                                 AiServiceProperties aiServiceProperties,
                                 ChannelSelector channelSelector,
                                 ChannelKeyCrypto channelKeyCrypto) {
        this.skillRepository = skillRepository;
        this.skillBindingRepository = skillBindingRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.promptVersionRepository = promptVersionRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.userProfileRepository = userProfileRepository;
        this.pipelineConfigService = pipelineConfigService;
        this.aiServiceClient = aiServiceClient;
        this.aiServiceProperties = aiServiceProperties;
        this.channelSelector = channelSelector;
        this.channelKeyCrypto = channelKeyCrypto;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> enhance(Map<String, Object> request, Long userId, Long tenantId) {
        Map<String, Object> enhanced = new HashMap<>(request);

        Long skillId = toLong(enhanced.remove("x_skill_id"));
        List<Long> kbIds = toLongList(enhanced.remove("x_knowledge_base_ids"));
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
                    List<SkillBinding> bindings = skillBindingRepository.findBySkillId(skillId);
                    for (SkillBinding b : bindings) {
                        if ("KNOWLEDGE_BASE".equals(b.getBindingType())) {
                            kbIds = kbIds != null ? new ArrayList<>(kbIds) : new ArrayList<>();
                            kbIds.add(b.getBindingId());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load skill {}: {}", skillId, e.getMessage());
            }
        }

        if (promptTemplateId != null) {
            appendPromptTemplate(systemPromptBuilder, promptTemplateId, promptVars);
        }

        if (kbIds != null && !kbIds.isEmpty()) {
            String userQuery = extractLastUserMessage(enhanced);
            if (userQuery != null && !userQuery.isBlank()) {
                appendRagContext(systemPromptBuilder, kbIds, tenantId, userId, userQuery);
            }
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
    private String extractLastUserMessage(Map<String, Object> request) {
        Object messages = request.get("messages");
        if (messages instanceof List<?> list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Object msg = list.get(i);
                if (msg instanceof Map<?, ?> map) {
                    if ("user".equals(map.get("role"))) {
                        Object content = map.get("content");
                        return content != null ? content.toString() : null;
                    }
                }
            }
        }
        return null;
    }

    private boolean containsKeywords(String text, String query) {
        String lower = text.toLowerCase();
        String[] words = query.toLowerCase().split("\\s+");
        int matched = 0;
        for (String w : words) {
            if (w.length() >= 2 && lower.contains(w)) matched++;
        }
        return matched > 0;
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
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


    private void appendRagContext(StringBuilder systemPromptBuilder, List<Long> kbIds,
                                  Long tenantId, Long userId, String userQuery) {
        StringBuilder ragContext = new StringBuilder();
        boolean vectorUsed = false;
        if (aiServiceProperties.isRag() && aiServiceClient.isHealthy()) {
            try {
                String embedModel = aiServiceProperties.getEmbeddingModel();
                var channel = channelSelector.selectForModel(embedModel, userId);
                Map<String, Object> upstream = new LinkedHashMap<>();
                upstream.put("base_url", channel.getBaseUrl());
                upstream.put("api_key", channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()));
                upstream.put("model", embedModel);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("query", userQuery);
                body.put("knowledge_base_ids", kbIds);
                body.put("top_k", 5);
                body.put("score_threshold", 0.3);
                body.put("embedding_model", embedModel);
                body.put("upstream", upstream);
                var chunks = aiServiceClient.queryRag(body);
                if (chunks.isPresent() && !chunks.get().isEmpty()) {
                    vectorUsed = true;
                    for (Map<String, Object> chunk : chunks.get()) {
                        Object content = chunk.get("content");
                        if (content == null) continue;
                        ragContext.append(truncate(content.toString(), 500)).append("\n\n");
                    }
                }
            } catch (Exception e) {
                log.warn("Vector RAG failed, fallback to keyword: {}", e.getMessage());
            }
        }
        if (!vectorUsed) {
            for (Long kbId : kbIds) {
                try {
                    List<KnowledgeDocument> docs = knowledgeBaseService.listDocuments(kbId, tenantId);
                    for (KnowledgeDocument doc : docs) {
                        if (doc.getContent() != null && containsKeywords(doc.getContent(), userQuery)) {
                            String snippet = truncate(doc.getContent(), 500);
                            ragContext.append("\u3010").append(doc.getTitle()).append("\u3011\n")
                                .append(snippet).append("\n\n");
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to search KB {}: {}", kbId, e.getMessage());
                }
            }
        }
        if (!ragContext.isEmpty()) {
            systemPromptBuilder.append("\n\n--- \u53c2\u8003\u8d44\u6599 ---\n").append(ragContext);
        }
    }    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<Long> toLongList(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) {
            return list.stream()
                .map(this::toLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        return null;
    }
}
