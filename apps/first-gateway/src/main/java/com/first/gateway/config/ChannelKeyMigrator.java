package com.first.gateway.config;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.repository.ChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ChannelKeyMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChannelKeyMigrator.class);

    private final ChannelRepository channelRepository;
    private final ChannelKeyCrypto channelKeyCrypto;

    public ChannelKeyMigrator(ChannelRepository channelRepository, ChannelKeyCrypto channelKeyCrypto) {
        this.channelRepository = channelRepository;
        this.channelKeyCrypto = channelKeyCrypto;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int migrated = 0;
            for (Channel channel : channelRepository.findAll()) {
                String raw = channel.getApiKeyEncrypted();
                if (raw == null || raw.isBlank() || channelKeyCrypto.looksLikeEncrypted(raw)) {
                    continue;
                }
                channel.setApiKeyEncrypted(channelKeyCrypto.encrypt(raw));
                channelRepository.save(channel);
                migrated++;
            }
            if (migrated > 0) {
                log.info("Migrated {} channel api keys from plaintext to AES-GCM", migrated);
            }
        } catch (Exception e) {
            log.error("Channel key migration failed: {}", e.getMessage());
        }
    }
}
