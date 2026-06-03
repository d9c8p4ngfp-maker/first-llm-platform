package com.first.gateway.service.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ChannelStatsService {

    private static final Logger log = LoggerFactory.getLogger(ChannelStatsService.class);
    private static final String SUCCESS_KEY = "channel:stats:%d:success";
    private static final String TOTAL_KEY = "channel:stats:%d:total";
    private static final long TTL_SECONDS = 300;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ConcurrentHashMap<Long, MemoryStats> memoryStats = new ConcurrentHashMap<>();

    public ChannelStatsService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public void record(Long channelId, boolean success) {
        if (channelId == null) {
            return;
        }
        if (recordRedis(channelId, success)) {
            return;
        }
        recordMemory(channelId, success);
    }

    public double getHealthFactor(Long channelId) {
        if (channelId == null) {
            return 1.0;
        }
        Double redisFactor = readRedisHealthFactor(channelId);
        if (redisFactor != null) {
            return redisFactor;
        }
        return readMemoryHealthFactor(channelId);
    }

    private boolean recordRedis(Long channelId, boolean success) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            String totalKey = TOTAL_KEY.formatted(channelId);
            redis.opsForValue().increment(totalKey);
            redis.expire(totalKey, java.time.Duration.ofSeconds(TTL_SECONDS));
            if (success) {
                String successKey = SUCCESS_KEY.formatted(channelId);
                redis.opsForValue().increment(successKey);
                redis.expire(successKey, java.time.Duration.ofSeconds(TTL_SECONDS));
            }
            return true;
        } catch (Exception ex) {
            log.warn("redis channel stats failed, fallback to memory: {}", ex.getMessage());
            return false;
        }
    }

    private Double readRedisHealthFactor(Long channelId) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            String successValue = redis.opsForValue().get(SUCCESS_KEY.formatted(channelId));
            String totalValue = redis.opsForValue().get(TOTAL_KEY.formatted(channelId));
            return computeHealthFactor(parseCount(successValue), parseCount(totalValue));
        } catch (Exception ex) {
            log.warn("redis channel stats read failed, fallback to memory: {}", ex.getMessage());
            return null;
        }
    }

    private void recordMemory(Long channelId, boolean success) {
        MemoryStats stats = memoryStats.computeIfAbsent(channelId, id -> new MemoryStats());
        stats.total.incrementAndGet();
        if (success) {
            stats.success.incrementAndGet();
        }
    }

    private double readMemoryHealthFactor(Long channelId) {
        MemoryStats stats = memoryStats.get(channelId);
        if (stats == null) {
            return 1.0;
        }
        return computeHealthFactor(stats.success.get(), stats.total.get());
    }

    static double computeHealthFactor(int success, int total) {
        if (total <= 0) {
            return 1.0;
        }
        double ratio = (double) success / total;
        if (ratio >= 0.8) {
            return 1.0;
        }
        if (ratio >= 0.5) {
            return ratio;
        }
        return 0.1;
    }

    private static int parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private static final class MemoryStats {
        private final AtomicInteger success = new AtomicInteger();
        private final AtomicInteger total = new AtomicInteger();
    }
}
