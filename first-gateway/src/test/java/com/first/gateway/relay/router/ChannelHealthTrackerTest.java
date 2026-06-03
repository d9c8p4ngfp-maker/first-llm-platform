package com.first.gateway.relay.router;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.enums.ChannelStatus;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.service.channel.ChannelStatsService;
import com.first.gateway.service.system.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelHealthTrackerTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private ChannelCircuitBreaker channelCircuitBreaker;
    @Mock
    private ChannelStatsService channelStatsService;

    private ChannelHealthTracker channelHealthTracker;

    @BeforeEach
    void setUp() {
        channelHealthTracker = new ChannelHealthTracker(
            channelRepository, systemConfigService, channelCircuitBreaker, channelStatsService);
    }

    @Test
    void recordSuccess_resetsFailCount() {
        Channel channel = channel(1L, ChannelStatus.ACTIVE, 3);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(channelRepository.save(channel)).thenReturn(channel);

        channelHealthTracker.recordSuccess(1L);

        assertEquals(0, channel.getFailCount());
        verify(channelRepository).save(channel);
        verify(channelStatsService).record(1L, true);
        verify(channelCircuitBreaker).recordSuccess(1L);
    }

    @Test
    void recordSuccess_skipsSaveWhenAlreadyZero() {
        Channel channel = channel(1L, ChannelStatus.ACTIVE, 0);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

        channelHealthTracker.recordSuccess(1L);

        verify(channelRepository, never()).save(channel);
    }

    @Test
    void recordFailure_incrementsCount() {
        Channel channel = channel(1L, ChannelStatus.ACTIVE, 0);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(systemConfigService.getInt("channel_fail_threshold", 5)).thenReturn(5);
        when(channelRepository.save(channel)).thenReturn(channel);

        channelHealthTracker.recordFailure(1L);

        assertEquals(1, channel.getFailCount());
        assertEquals(ChannelStatus.ACTIVE, channel.getStatus());
        verify(channelStatsService).record(1L, false);
        verify(channelCircuitBreaker).recordFailure(1L);
    }

    @Test
    void recordFailure_triggersAutoDisable() {
        Channel channel = channel(1L, ChannelStatus.ACTIVE, 4);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(systemConfigService.getInt("channel_fail_threshold", 5)).thenReturn(5);
        when(channelRepository.save(channel)).thenReturn(channel);

        channelHealthTracker.recordFailure(1L);

        assertEquals(5, channel.getFailCount());
        assertEquals(ChannelStatus.AUTO_DISABLED, channel.getStatus());
    }

    @Test
    void recordFailure_respectsThresholdFromConfig() {
        Channel channel = channel(1L, ChannelStatus.ACTIVE, 2);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(systemConfigService.getInt("channel_fail_threshold", 5)).thenReturn(3);
        when(channelRepository.save(channel)).thenReturn(channel);

        channelHealthTracker.recordFailure(1L);

        assertEquals(3, channel.getFailCount());
        assertEquals(ChannelStatus.AUTO_DISABLED, channel.getStatus());
    }

    @Test
    void recordFailure_noOpForNonExistentChannel() {
        when(channelRepository.findById(99L)).thenReturn(Optional.empty());

        channelHealthTracker.recordFailure(99L);

        verify(channelRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordFailure_skipsNonActive() {
        Channel channel = channel(1L, ChannelStatus.DISABLED, 0);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(systemConfigService.getInt("channel_fail_threshold", 5)).thenReturn(3);
        when(channelRepository.save(channel)).thenReturn(channel);

        channelHealthTracker.recordFailure(1L);

        assertEquals(1, channel.getFailCount());
        assertEquals(ChannelStatus.DISABLED, channel.getStatus());
    }

    private static Channel channel(long id, ChannelStatus status, int failCount) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setStatus(status);
        channel.setFailCount(failCount);
        return channel;
    }
}
