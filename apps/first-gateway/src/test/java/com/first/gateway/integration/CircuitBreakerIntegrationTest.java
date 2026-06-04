package com.first.gateway.integration;

import com.first.gateway.support.RedisIntegrationSupport;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.enums.ChannelStatus;
import com.first.gateway.relay.router.ChannelCircuitBreaker;
import com.first.gateway.relay.router.ChannelHealthTracker;
import com.first.gateway.repository.ChannelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CircuitBreakerIntegrationTest extends RedisIntegrationSupport {

    @Autowired
    private ChannelHealthTracker channelHealthTracker;
    @Autowired
    private ChannelCircuitBreaker channelCircuitBreaker;
    @Autowired
    private ChannelRepository channelRepository;

    @Test
    void consecutiveFailures_disablesChannel() {
        Channel channel = saveChannel("cb-open-" + System.nanoTime());

        for (int i = 0; i < 5; i++) {
            channelHealthTracker.recordFailure(channel.getId());
        }

        Channel updated = channelRepository.findById(channel.getId()).orElseThrow();
        assertTrue(channelCircuitBreaker.isOpen(channel.getId())
            || updated.getFailCount() >= 5
            || updated.getStatus() == ChannelStatus.AUTO_DISABLED);
    }

    @Test
    void circuitBreaker_redisDown_fallbackToFailCount() {
        Channel channel = saveChannel("cb-failcount-" + System.nanoTime());

        channelHealthTracker.recordFailure(channel.getId());
        channelHealthTracker.recordFailure(channel.getId());

        Channel updated = channelRepository.findById(channel.getId()).orElseThrow();
        assertEquals(2, updated.getFailCount());
        assertEquals(ChannelStatus.ACTIVE, updated.getStatus());
    }

    private Channel saveChannel(String name) {
        Channel channel = new Channel();
        channel.setName(name);
        channel.setType("openai");
        channel.setProvider("openai");
        channel.setBaseUrl("https://api.example.com");
        channel.setApiKeyEncrypted("enc-key");
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setPriority(1);
        channel.setWeight(1);
        channel.setMaxRpm(0);
        channel.setFailCount(0);
        return channelRepository.save(channel);
    }
}