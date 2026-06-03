package com.first.gateway.relay.router;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.enums.ChannelStatus;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.service.channel.ChannelStatsService;
import com.first.gateway.service.system.SystemConfigService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChannelHealthTracker {

    private final ChannelRepository channelRepository;
    private final SystemConfigService systemConfigService;
    private final ChannelCircuitBreaker channelCircuitBreaker;
    private final ChannelStatsService channelStatsService;

    public ChannelHealthTracker(ChannelRepository channelRepository,
                                SystemConfigService systemConfigService,
                                ChannelCircuitBreaker channelCircuitBreaker,
                                ChannelStatsService channelStatsService) {
        this.channelRepository = channelRepository;
        this.systemConfigService = systemConfigService;
        this.channelCircuitBreaker = channelCircuitBreaker;
        this.channelStatsService = channelStatsService;
    }

    @Transactional
    public void recordSuccess(Long channelId) {
        channelStatsService.record(channelId, true);
        channelCircuitBreaker.recordSuccess(channelId);
        channelRepository.findById(channelId).ifPresent(channel -> {
            if (channel.getFailCount() != null && channel.getFailCount() > 0) {
                channel.setFailCount(0);
                channelRepository.save(channel);
            }
        });
    }

    @Transactional
    public void recordFailure(Long channelId) {
        channelStatsService.record(channelId, false);
        channelCircuitBreaker.recordFailure(channelId);
        channelRepository.findById(channelId).ifPresent(channel -> {
            int fails = (channel.getFailCount() != null ? channel.getFailCount() : 0) + 1;
            channel.setFailCount(fails);
            int threshold = systemConfigService.getInt("channel_fail_threshold", 5);
            if (fails >= threshold && channel.getStatus() == ChannelStatus.ACTIVE) {
                channel.setStatus(ChannelStatus.AUTO_DISABLED);
            }
            channelRepository.save(channel);
        });
    }
}
