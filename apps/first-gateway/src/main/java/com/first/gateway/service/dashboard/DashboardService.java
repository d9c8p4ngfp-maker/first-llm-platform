package com.first.gateway.service.dashboard;

import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.service.log.StatsService;
import com.first.gateway.service.profile.UserMemoryService;
import com.first.gateway.service.profile.UserProfileService;
import com.first.gateway.service.stats.RedisStatsService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final StatsService statsService;
    private final UserProfileService userProfileService;
    private final UserMemoryService userMemoryService;
    private final RedisStatsService redisStatsService;

    public DashboardService(StatsService statsService,
                            UserProfileService userProfileService,
                            UserMemoryService userMemoryService,
                            @Lazy RedisStatsService redisStatsService) {
        this.statsService = statsService;
        this.userProfileService = userProfileService;
        this.userMemoryService = userMemoryService;
        this.redisStatsService = redisStatsService;
    }

    public Map<String, Object> realtime(Long tenantId, Long userId, String username) {
        Map<String, Object> todayStats;
        if (redisStatsService != null) {
            todayStats = redisStatsService.getTodayStats(userId, tenantId);
        } else {
            LocalDate today = LocalDate.now();
            List<StatsService.DailyStat> todayRows = statsService.dailyStats(today, today, tenantId, null);
            long requests = todayRows.stream().mapToLong(StatsService.DailyStat::requestCount).sum();
            long tokens = todayRows.stream().mapToLong(StatsService.DailyStat::totalTokens).sum();
            todayStats = new LinkedHashMap<>();
            todayStats.put("requests", requests);
            todayStats.put("tokens", tokens);
            todayStats.put("cost", 0);
            todayStats.put("cost_currency", "CNY");
        }

        List<UserMemory> events = userMemoryService.recentEvents(userId, 5);
        List<Map<String, Object>> highlights = events.stream()
            .filter(e -> e.getNumericValue() != null)
            .map(e -> {
                Map<String, Object> h = new LinkedHashMap<>();
                h.put("label", e.getContent());
                h.put("value", e.getNumericValue());
                return h;
            })
            .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("profile_summary", userProfileService.profileSummary(userId, tenantId, username));
        body.put("today_schedule", userMemoryService.todaySchedule(userId));
        body.put("upcoming_schedule", userMemoryService.upcomingSchedule(userId));
        body.put("today_stats", todayStats);
        body.put("business_highlights", highlights);
        return body;
    }
}