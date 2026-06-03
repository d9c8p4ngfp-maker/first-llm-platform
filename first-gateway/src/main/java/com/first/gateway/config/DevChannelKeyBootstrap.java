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

    public DevChannelKeyBootstrap(ChannelRepository channelRepository,
                                  ChannelKeyCrypto channelKeyCrypto,
                                  @Value("${gateway.upstream.deepseek-api-key:}") String deepseekApiKey) {
        this.channelRepository = channelRepository;
        this.channelKeyCrypto = channelKeyCrypto;
        this.deepseekApiKey = deepseekApiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        String resolvedKey = resolveDeepseekApiKey();
        if (resolvedKey == null || resolvedKey.isBlank()) {
            log.warn("DEEPSEEK_API_KEY not set; channel upstream key must be configured in database");
            return;
        }
        channelRepository.findById(1L).ifPresentOrElse(channel -> updateIfNeeded(channel, resolvedKey),
            () -> log.warn("Default channel id=1 not found; skip DEEPSEEK_API_KEY bootstrap"));
    }

    private String resolveDeepseekApiKey() {
        if (deepseekApiKey != null && !deepseekApiKey.isBlank()) {
            return deepseekApiKey;
        }
        String fromEnv = System.getenv("DEEPSEEK_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return readFromDotEnv("DEEPSEEK_API_KEY");
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

    private void updateIfNeeded(Channel channel, String plainKey) {
        String encrypted = channelKeyCrypto.encrypt(plainKey);
        if (encrypted.equals(channel.getApiKeyEncrypted())) {
            return;
        }
        channel.setApiKeyEncrypted(encrypted);
        channelRepository.save(channel);
        log.info("Updated channel '{}' upstream API key from DEEPSEEK_API_KEY", channel.getName());
    }
}