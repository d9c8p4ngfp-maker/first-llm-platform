package com.first.gateway.service.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChannelStatsServiceTest {

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private ChannelStatsService statsService;

    @BeforeEach
    void setUp() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);
        statsService = new ChannelStatsService(redisTemplateProvider);
    }

    @Test
    void record_success_incrementsBothKeys() {
        statsService.record(1L, true);

        assertEquals(1.0, statsService.getHealthFactor(1L));
    }

    @Test
    void record_failure_incrementsOnlyTotal() {
        statsService.record(1L, false);

        assertEquals(0.1, statsService.getHealthFactor(1L));
    }

    @Test
    void getHealthFactor_noData_returnsOne() {
        assertEquals(1.0, statsService.getHealthFactor(99L));
        assertEquals(1.0, ChannelStatsService.computeHealthFactor(0, 0));
    }

    @Test
    void getHealthFactor_highSuccess_returnsOne() {
        assertEquals(1.0, ChannelStatsService.computeHealthFactor(9, 10));
    }

    @Test
    void getHealthFactor_mediumSuccess_returnsActual() {
        assertEquals(0.7, ChannelStatsService.computeHealthFactor(7, 10));
    }

    @Test
    void getHealthFactor_lowSuccess_returnsMinimum() {
        assertEquals(0.1, ChannelStatsService.computeHealthFactor(3, 10));
    }
}