package com.first.gateway.service.prompt;

import com.first.gateway.domain.entity.PromptFavorite;
import com.first.gateway.domain.entity.PromptTemplate;
import com.first.gateway.domain.entity.PromptVersion;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.PromptFavoriteRepository;
import com.first.gateway.repository.PromptTemplateRepository;
import com.first.gateway.repository.PromptVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PromptTemplateService {

    private final PromptTemplateRepository templateRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptFavoriteRepository favoriteRepository;

    public PromptTemplateService(PromptTemplateRepository templateRepository,
                                 PromptVersionRepository versionRepository,
                                 PromptFavoriteRepository favoriteRepository) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.favoriteRepository = favoriteRepository;
    }

    public List<PromptTemplate> list(Long tenantId) {
        return templateRepository.findByTenantIdAndDeletedOrderByUpdatedAtDesc(tenantId, (short) 0);
    }

    public PromptTemplate require(Long id, Long tenantId) {
        return templateRepository.findByIdAndTenantIdAndDeleted(id, tenantId, (short) 0)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "prompt template not found"));
    }

    public List<PromptVersion> versions(Long id, Long tenantId) {
        require(id, tenantId);
        return versionRepository.findByTemplateIdOrderByCreatedAtDesc(id);
    }

    public List<PromptTemplate> favorites(Long userId) {
        return favoriteRepository.findByUserId(userId).stream()
            .map(PromptFavorite::getPromptTemplateId)
            .map(templateRepository::findById)
            .flatMap(java.util.Optional::stream)
            .filter(t -> t.getDeleted() == 0)
            .toList();
    }

    @Transactional
    public PromptTemplate create(Long tenantId, Long userId, String name, String description,
                                 String industry, String category, String visibility,
                                 String systemPrompt, String userPromptTemplate,
                                 String variables, String suggestedModel) {
        PromptTemplate t = new PromptTemplate();
        t.setTenantId(tenantId);
        t.setUserId(userId);
        t.setName(name != null && !name.isBlank() ? name.trim() : "Untitled");
        t.setDescription(description);
        t.setIndustry(industry);
        t.setCategory(category);
        t.setVisibility(visibility != null ? visibility : "PRIVATE");
        t.setDeleted((short) 0);
        t = templateRepository.save(t);
        PromptVersion v = newVersion(t.getId(), systemPrompt, userPromptTemplate, variables, suggestedModel, userId);
        v = versionRepository.save(v);
        t.setCurrentVersionId(v.getId());
        return templateRepository.save(t);
    }

    @Transactional
    public PromptTemplate update(Long id, Long tenantId, Long userId, String name, String description,
                                 String industry, String category, String visibility,
                                 String systemPrompt, String userPromptTemplate,
                                 String variables, String suggestedModel) {
        PromptTemplate t = require(id, tenantId);
        if (name != null && !name.isBlank()) t.setName(name.trim());
        if (description != null) t.setDescription(description);
        if (industry != null) t.setIndustry(industry);
        if (category != null) t.setCategory(category);
        if (visibility != null) t.setVisibility(visibility);
        if (systemPrompt != null || userPromptTemplate != null) {
            PromptVersion v = newVersion(id, systemPrompt, userPromptTemplate, variables, suggestedModel, userId);
            v = versionRepository.save(v);
            t.setCurrentVersionId(v.getId());
        }
        return templateRepository.save(t);
    }

    @Transactional
    public void delete(Long id, Long tenantId) {
        PromptTemplate t = require(id, tenantId);
        t.setDeleted((short) 1);
        templateRepository.save(t);
    }

    @Transactional
    public Map<String, Object> toggleFavorite(Long id, Long userId, Long tenantId) {
        require(id, tenantId);
        var existing = favoriteRepository.findByUserIdAndPromptTemplateId(userId, id);
        boolean favorited;
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            favorited = false;
        } else {
            PromptFavorite f = new PromptFavorite();
            f.setUserId(userId);
            f.setPromptTemplateId(id);
            favoriteRepository.save(f);
            favorited = true;
        }
        return Map.of("favorited", favorited);
    }

    public Map<String, Object> preview(Long id, Long tenantId, Map<String, Object> variables) {
        PromptTemplate t = require(id, tenantId);
        String rendered = versionRepository.findFirstByTemplateIdOrderByCreatedAtDesc(id)
            .map(PromptVersion::getUserPromptTemplate)
            .orElse("");
        if (variables != null) {
            for (var entry : variables.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("template_id", t.getId());
        body.put("rendered", rendered);
        return body;
    }

    @Transactional
    public PromptTemplate rollback(Long id, Long tenantId, Long versionId) {
        PromptTemplate t = require(id, tenantId);
        PromptVersion v = versionRepository.findById(versionId)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "version not found"));
        if (!id.equals(v.getTemplateId())) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "version does not belong to this template");
        }
        t.setCurrentVersionId(versionId);
        return templateRepository.save(t);
    }

    private PromptVersion newVersion(Long templateId, String systemPrompt, String userPromptTemplate,
                                     String variables, String suggestedModel, Long userId) {
        PromptVersion v = new PromptVersion();
        v.setTemplateId(templateId);
        v.setVersion("v" + System.currentTimeMillis());
        v.setSystemPrompt(systemPrompt);
        v.setUserPromptTemplate(userPromptTemplate);
        v.setVariables(variables);
        v.setSuggestedModel(suggestedModel);
        v.setCreatedBy(userId);
        return v;
    }
}