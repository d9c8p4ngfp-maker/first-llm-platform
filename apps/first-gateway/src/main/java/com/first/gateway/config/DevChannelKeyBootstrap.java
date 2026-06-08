package com.first.gateway.config;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.repository.ChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DevChannelKeyBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevChannelKeyBootstrap.class);

    private final ChannelRepository channelRepository;
    private final ChannelKeyCrypto channelKeyCrypto;
    private final String deepseekApiKey;
    private final String bailianApiKey;

    public DevChannelKeyBootstrap(ChannelRepository channelRepository,
                                  ChannelKeyCrypto channelKeyCrypto,
                                  @Value("${gateway.upstream.deepseek-api-key:}") String deepseekApiKey,
                                  @Value("${gateway.upstream.bailian-api-key:}") String bailianApiKey) {
        this.channelRepository = channelRepository;
        this.channelKeyCrypto = channelKeyCrypto;
        this.deepseekApiKey = deepseekApiKey;
        this.bailianApiKey = bailianApiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapChannelKey(1L, resolveKey(deepseekApiKey, "DEEPSEEK_API_KEY"), "DeepSeek");
        bootstrapChannelKey(3L, resolveKey(bailianApiKey, "BAILIAN_API_KEY"), "Bailian");
    }

    private void bootstrapChannelKey(Long channelId, String plainKey, String label) {
        if (plainKey == null || plainKey.isBlank()) {
            log.warn("{}_API_KEY not set; channel {} key must be configured via API", label.toUpperCase(), channelId);
            return;
        }
        channelRepository.findById(channelId).ifPresentOrElse(
            channel -> updateIfNeeded(channel, plainKey, label),
            () -> log.warn("Channel id={} not found; skip {} API_KEY bootstrap", channelId, label));
    }

    private String resolveKey(String configured, String envName) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return readFromDotEnv(envName);
    }

    private String readFromDotEnv(String key) {
        for (Path envPath : new Path[] { Path.of("../.env"), Path.of(".env") }) {
            if (!Files.isRegularFile(envPath)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(envPath)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    int eq = trimmed.indexOf('=');
                    if (eq > 0 && trimmed.substring(0, eq).trim().equals(key)) {
                        return trimmed.substring(eq + 1).trim();
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to read {}: {}", envPath, e.getMessage());
            }
        }
        return null;
    }

    private void updateIfNeeded(Channel channel, String plainKey, String label) {
        String encrypted = channelKeyCrypto.encrypt(plainKey);
        if (encrypted.equals(channel.getApiKeyEncrypted())) {
            return;
        }
        channel.setApiKeyEncrypted(encrypted);
        channelRepository.save(channel);
        log.info("Updated channel '{}' upstream API key from {}_API_KEY", channel.getName(), label.toUpperCase());
    }
}