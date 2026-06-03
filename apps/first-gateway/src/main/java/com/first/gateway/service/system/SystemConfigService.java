package com.first.gateway.service.system;

import com.first.gateway.domain.entity.SystemConfig;
import com.first.gateway.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    public Map<String, String> allSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        systemConfigRepository.findAll().forEach(c -> settings.put(c.getConfigKey(), c.getConfigValue()));
        return settings;
    }

    @Transactional
    public Map<String, String> updateSettings(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            SystemConfig config = systemConfigRepository.findByConfigKey(entry.getKey())
                .orElseGet(() -> {
                    SystemConfig c = new SystemConfig();
                    c.setConfigKey(entry.getKey());
                    return c;
                });
            config.setConfigValue(entry.getValue());
            systemConfigRepository.save(config);
            cache.put(entry.getKey(), entry.getValue());
        }
        return allSettings();
    }

    public String getString(String key, String defaultValue) {
        return cache.computeIfAbsent(key, k -> systemConfigRepository.findByConfigKey(k)
            .map(SystemConfig::getConfigValue)
            .orElse(defaultValue));
    }

    public long getLong(String key, long defaultValue) {
        String raw = getString(key, Long.toString(defaultValue));
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        String raw = getString(key, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
