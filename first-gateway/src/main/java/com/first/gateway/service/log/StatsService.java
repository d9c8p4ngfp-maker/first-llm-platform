package com.first.gateway.service.log;

import com.first.gateway.repository.TokenUsageLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final TokenUsageLogRepository tokenUsageLogRepository;

    public StatsService(TokenUsageLogRepository tokenUsageLogRepository) {
        this.tokenUsageLogRepository = tokenUsageLogRepository;
    }

    public List<DailyStat> dailyStats(LocalDate startDate,
                                      LocalDate endDate,
                                      Long tenantId,
                                      String model) {
        return tokenUsageLogRepository.dailyStats(startDate, endDate, tenantId, model).stream()
            .map(row -> new DailyStat(
                row.getStatDay(),
                row.getRequestCount(),
                row.getPromptTokens(),
                row.getCompletionTokens(),
                row.getTotalTokens()))
            .toList();
    }

    public Map<String, Object> summary(Long tenantId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        List<DailyStat> daily = dailyStats(startDate, endDate, tenantId, null);
        long totalRequests = daily.stream().mapToLong(DailyStat::requestCount).sum();
        long totalTokens = daily.stream().mapToLong(DailyStat::totalTokens).sum();
        return Map.of(
            "total_requests", totalRequests,
            "total_tokens", totalTokens,
            "period_days", 30
        );
    }

    public record DailyStat(
        LocalDate date,
        long requestCount,
        long promptTokens,
        long completionTokens,
        long totalTokens
    ) {}
}
