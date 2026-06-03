package com.first.gateway.service.channel;

import com.first.gateway.domain.entity.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ChannelRpmGuard {

    private static final Logger log = LoggerFactory.getLogger(ChannelRpmGuard.class);
    private static final String RPM_KEY = "channel:rpm:%d:%d";
    private static final long TTL_SECONDS = 120;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ConcurrentHashMap<String, AtomicInteger> memoryCounters = new ConcurrentHashMap<>();

    public ChannelRpmGuard(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public boolean isAvailable(Channel channel) {
        if (channel == null || !hasLimit(channel)) {
            return true;
        }
        int current = currentCount(channel);
        return current < channel.getMaxRpm();
    }

    public boolean acquire(Channel channel) {
        if (channel == null || !hasLimit(channel)) {
            return true;
        }
        if (acquireRedis(channel)) {
            return true;
        }
        return acquireMemory(channel);
    }

    private boolean hasLimit(Channel channel) {
        return channel.getMaxRpm() != null && channel.getMaxRpm() > 0;
    }

    private int currentCount(Channel channel) {
        Integer redisCount = readRedisCount(channel);
        if (redisCount != null) {
            return redisCount;
        }
        return readMemoryCount(channel);
    }

    private boolean acquireRedis(Channel channel) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            String key = rpmKey(channel);
            Long count = redis.opsForValue().increment(key);
            redis.expire(key, java.time.Duration.ofSeconds(TTL_SECONDS));
            if (count != null && count > channel.getMaxRpm()) {
                redis.opsForValue().decrement(key);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("redis channel rpm failed, fallback to memory: {}", ex.getMessage());
            return false;
        }
    }

    private Integer readRedisCount(Channel channel) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            String value = redis.opsForValue().get(rpmKey(channel));
            return value == null ? 0 : Integer.parseInt(value);
        } catch (Exception ex) {
            log.warn("redis channel rpm read failed, fallback to memory: {}", ex.getMessage());
            return null;
        }
    }

    private boolean acquireMemory(Channel channel) {
        AtomicInteger counter = memoryCounters.computeIfAbsent(rpmKey(channel), key -> new AtomicInteger());
        int next = counter.incrementAndGet();
        if (next > channel.getMaxRpm()) {
            counter.decrementAndGet();
            return false;
        }
        return true;
    }

    private int readMemoryCount(Channel channel) {
        AtomicInteger counter = memoryCounters.get(rpmKey(channel));
        return counter == null ? 0 : counter.get();
    }

    private static String rpmKey(Channel channel) {
        long minute = Instant.now().getEpochSecond() / 60;
        return RPM_KEY.formatted(channel.getId(), minute);
    }
}
