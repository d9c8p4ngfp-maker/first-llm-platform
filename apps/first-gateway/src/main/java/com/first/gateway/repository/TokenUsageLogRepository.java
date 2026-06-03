package com.first.gateway.repository;

import com.first.gateway.domain.entity.TokenUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TokenUsageLogRepository extends JpaRepository<TokenUsageLog, Long>,
        JpaSpecificationExecutor<TokenUsageLog> {

    List<TokenUsageLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<TokenUsageLog> findByRequestId(String requestId);

    @Query(value = """
        SELECT CAST(created_at AS DATE) AS stat_day,
               COUNT(*) AS request_count,
               COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens,
               COALESCE(SUM(completion_tokens), 0) AS completion_tokens,
               COALESCE(SUM(total_tokens), 0) AS total_tokens
        FROM token_usage_log
        WHERE CAST(created_at AS DATE) BETWEEN :startDate AND :endDate
          AND (COALESCE(:tenantId, tenant_id) = tenant_id)
          AND (COALESCE(:model, model) = model)
        GROUP BY CAST(created_at AS DATE)
        ORDER BY stat_day
        """, nativeQuery = true)
    List<DailyStatProjection> dailyStats(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("tenantId") Long tenantId,
                                         @Param("model") String model);

    interface DailyStatProjection {
        LocalDate getStatDay();

        long getRequestCount();

        long getPromptTokens();

        long getCompletionTokens();

        long getTotalTokens();
    }
}
