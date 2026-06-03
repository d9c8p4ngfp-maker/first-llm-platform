package com.first.gateway.service.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.ConsoleUserPreference;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ConsoleUserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ConsolePreferenceService {

    private final ChannelModelRepository channelModelRepository;
    private final ConsoleUserPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    public ConsolePreferenceService(ChannelModelRepository channelModelRepository,
                                    ConsoleUserPreferenceRepository preferenceRepository,
                                    ObjectMapper objectMapper) {
        this.channelModelRepository = channelModelRepository;
        this.preferenceRepository = preferenceRepository;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> listModelsForUser(Long userId) {
        List<ChannelModel> models = channelModelRepository.findEnabledByUserId(userId);
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChannelModel cm : models) {
            if (!seen.add(cm.getModelName())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", cm.getModelName());
            row.put("name", cm.getModelName());
            row.put("alias", cm.getModelAlias());
            row.put("tier", cm.getTier());
            row.put("channel_id", cm.getChannelId());
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> getModelPreferences(Long userId) {
        ConsoleUserPreference pref = preferenceRepository.findById(userId).orElse(null);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("default_model", pref != null ? pref.getDefaultModel() : null);
        body.put("routing_priority", extractFromPreferencesJson(pref, "routing_priority"));
        return body;
    }

    @Transactional
    public Map<String, Object> saveModelPreferences(Long userId, String defaultModel) {
        ConsoleUserPreference pref = preferenceRepository.findById(userId).orElseGet(() -> {
            ConsoleUserPreference created = new ConsoleUserPreference();
            created.setUserId(userId);
            return created;
        });
        pref.setDefaultModel(defaultModel);
        preferenceRepository.save(pref);
        return getModelPreferences(userId);
    }

    public Map<String, Object> getSettings(Long userId) {
        ConsoleUserPreference pref = preferenceRepository.findById(userId).orElse(null);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("default_model", pref != null ? pref.getDefaultModel() : null);
        body.put("theme", extractFromPreferencesJson(pref, "theme"));
        body.put("language", extractFromPreferencesJson(pref, "language"));
        body.put("routing_priority", extractFromPreferencesJson(pref, "routing_priority"));
        return body;
    }

    @Transactional
    public Map<String, Object> saveSettings(Long userId, String defaultModel, String theme, String language) {
        ConsoleUserPreference pref = preferenceRepository.findById(userId).orElseGet(() -> {
            ConsoleUserPreference created = new ConsoleUserPreference();
            created.setUserId(userId);
            return created;
        });
        if (defaultModel != null) {
            pref.setDefaultModel(defaultModel.isBlank() ? null : defaultModel);
        }
        Map<String, Object> prefs = parsePreferencesJson(pref);
        if (theme != null) {
            prefs.put("theme", theme);
        }
        if (language != null) {
            prefs.put("language", language);
        }
        pref.setPreferencesJson(toJson(prefs));
        preferenceRepository.save(pref);
        return getSettings(userId);
    }

    @Transactional
    public Map<String, Object> saveModelPreferencesWithRouting(Long userId, String defaultModel, String routingPriority) {
        ConsoleUserPreference pref = preferenceRepository.findById(userId).orElseGet(() -> {
            ConsoleUserPreference created = new ConsoleUserPreference();
            created.setUserId(userId);
            return created;
        });
        if (defaultModel != null) {
            pref.setDefaultModel(defaultModel);
        }
        if (routingPriority != null) {
            Map<String, Object> prefs = parsePreferencesJson(pref);
            prefs.put("routing_priority", routingPriority);
            pref.setPreferencesJson(toJson(prefs));
        }
        preferenceRepository.save(pref);
        return getModelPreferences(userId);
    }

    private Map<String, Object> parsePreferencesJson(ConsoleUserPreference pref) {
        if (pref == null || pref.getPreferencesJson() == null || pref.getPreferencesJson().isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(pref.getPreferencesJson(), Map.class);
            return new LinkedHashMap<>(map);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Object extractFromPreferencesJson(ConsoleUserPreference pref, String key) {
        Map<String, Object> prefs = parsePreferencesJson(pref);
        return prefs.get(key);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}