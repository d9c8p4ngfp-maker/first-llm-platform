package com.first.gateway.service.channel;

import com.first.gateway.domain.entity.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelRpmGuardTest {

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private ChannelRpmGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ChannelRpmGuard(redisTemplateProvider);
    }

    @Test
    void acquire_underLimit_returnsTrue() {
        Channel channel = channelWithMaxRpm(10);
        for (int i = 0; i < 5; i++) {
            assertTrue(guard.acquire(channel));
        }
    }

    @Test
    void acquire_atLimit_returnsFalse() {
        Channel channel = channelWithMaxRpm(10);
        for (int i = 0; i < 10; i++) {
            assertTrue(guard.acquire(channel));
        }

        assertFalse(guard.acquire(channel));
    }

    @Test
    void acquire_zeroMaxRpm_alwaysAllows() {
        Channel channel = channelWithMaxRpm(0);

        assertTrue(guard.acquire(channel));
        assertTrue(guard.acquire(channel));
    }

    @Test
    void acquire_negativeMaxRpm_alwaysAllows() {
        Channel channel = channelWithMaxRpm(-1);

        assertTrue(guard.acquire(channel));
        assertTrue(guard.acquire(channel));
    }

    @Test
    void acquire_redisDown_returnsTrue() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        Channel channel = channelWithMaxRpm(10);

        assertTrue(guard.acquire(channel));
    }

    private static Channel channelWithMaxRpm(int maxRpm) {
        Channel channel = new Channel();
        channel.setId(1L);
        channel.setMaxRpm(maxRpm);
        return channel;
    }
}