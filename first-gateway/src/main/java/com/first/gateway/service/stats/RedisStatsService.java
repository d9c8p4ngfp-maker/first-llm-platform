package com.first.gateway.service.stats;

import com.first.gateway.service.log.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RedisStatsService {

    private static final Logger log = LoggerFactory.getLogger(RedisStatsService.class);

    private final StringRedisTemplate redisTemplate;
    private final StatsService statsService;

    public RedisStatsService(@Autowired(required = false) StringRedisTemplate redisTemplate,
                              StatsService statsService) {
        this.redisTemplate = redisTemplate;
        this.statsService = statsService;
    }

    public void incrementAfterChat(Long userId, int totalTokens, double cost) {
        if (redisTemplate == null) {
            log.trace("Redis unavailable, stats only in DB");
            return;
        }
        try {
            String date = LocalDate.now().toString();
            String prefix = "user:" + userId + ":stats:" + date;
            redisTemplate.opsForValue().increment(prefix + ":requests");
            redisTemplate.opsForValue().increment(prefix + ":tokens", totalTokens);
            redisTemplate.expire(prefix + ":requests", Duration.ofHours(48));
            redisTemplate.expire(prefix + ":tokens", Duration.ofHours(48));
        } catch (Exception e) {
            log.warn("Redis stats increment failed: {}", e.getMessage());
        }
    }

    public Map<String, Object> getTodayStats(Long userId, Long tenantId) {
        if (redisTemplate != null) {
            try {
                String date = LocalDate.now().toString();
                String prefix = "user:" + userId + ":stats:" + date;
                String reqStr = redisTemplate.opsForValue().get(prefix + ":requests");
                String tokenStr = redisTemplate.opsForValue().get(prefix + ":tokens");
                if (reqStr != null || tokenStr != null) {
                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("requests", reqStr != null ? Long.parseLong(reqStr) : 0L);
                    stats.put("tokens", tokenStr != null ? Long.parseLong(tokenStr) : 0L);
                    stats.put("cost", 0);
                    stats.put("cost_currency", "CNY");
                    stats.put("source", "redis");
                    return stats;
                }
            } catch (Exception e) {
                log.warn("Redis stats read failed, falling back to DB: {}", e.getMessage());
            }
        }

        LocalDate today = LocalDate.now();
        List<StatsService.DailyStat> rows = statsService.dailyStats(today, today, tenantId, null);
        long requests = rows.stream().mapToLong(StatsService.DailyStat::requestCount).sum();
        long tokens = rows.stream().mapToLong(StatsService.DailyStat::totalTokens).sum();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("requests", requests);
        stats.put("tokens", tokens);
        stats.put("cost", 0);
        stats.put("cost_currency", "CNY");
        stats.put("source", "db");
        return stats;
    }
}
