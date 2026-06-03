package com.first.gateway.service.system;

import com.first.gateway.domain.entity.SystemConfig;
import com.first.gateway.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigService(systemConfigRepository);
    }

    @Test
    void getString_returnsDbValue() {
        when(systemConfigRepository.findByConfigKey("foo"))
            .thenReturn(Optional.of(config("foo", "bar")));

        assertEquals("bar", systemConfigService.getString("foo", "default"));
    }

    @Test
    void getString_returnsDefault() {
        when(systemConfigRepository.findByConfigKey("missing")).thenReturn(Optional.empty());

        assertEquals("default", systemConfigService.getString("missing", "default"));
    }

    @Test
    void getLong_parsesCorrectly() {
        when(systemConfigRepository.findByConfigKey("num"))
            .thenReturn(Optional.of(config("num", "100")));

        assertEquals(100L, systemConfigService.getLong("num", 0L));
    }

    @Test
    void getLong_fallsBackOnInvalid() {
        when(systemConfigRepository.findByConfigKey("bad"))
            .thenReturn(Optional.of(config("bad", "abc")));

        assertEquals(42L, systemConfigService.getLong("bad", 42L));
    }

    @Test
    void updateSettings_createsNewKey() {
        when(systemConfigRepository.findByConfigKey("new_key")).thenReturn(Optional.empty());
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        when(systemConfigRepository.findAll()).thenReturn(java.util.List.of(config("new_key", "v1")));

        Map<String, String> result = systemConfigService.updateSettings(Map.of("new_key", "v1"));

        assertEquals("v1", result.get("new_key"));
        assertEquals("v1", systemConfigService.getString("new_key", "x"));
    }

    @Test
    void updateSettings_updatesExistingKey() {
        SystemConfig existing = config("k", "old");
        when(systemConfigRepository.findByConfigKey("k")).thenReturn(Optional.of(existing));
        when(systemConfigRepository.save(existing)).thenReturn(existing);
        when(systemConfigRepository.findAll()).thenReturn(java.util.List.of(config("k", "new")));

        Map<String, String> result = systemConfigService.updateSettings(Map.of("k", "new"));

        assertEquals("new", result.get("k"));
        assertEquals("new", systemConfigService.getString("k", "x"));
    }

    private static SystemConfig config(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
