package com.first.gateway.service.log;

import com.first.gateway.domain.entity.TokenUsageLog;
import com.first.gateway.repository.TokenUsageLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LogQueryService {

    private final TokenUsageLogRepository tokenUsageLogRepository;

    public LogQueryService(TokenUsageLogRepository tokenUsageLogRepository) {
        this.tokenUsageLogRepository = tokenUsageLogRepository;
    }

    public Page<TokenUsageLog> search(Long tenantId,
                                      Long apiKeyId,
                                      String model,
                                      String status,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      Pageable pageable) {
        Specification<TokenUsageLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            if (apiKeyId != null) {
                predicates.add(cb.equal(root.get("apiKeyId"), apiKeyId));
            }
            if (model != null && !model.isBlank()) {
                predicates.add(cb.equal(root.get("model"), model));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (endDate != null) {
                Instant end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.lessThan(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return tokenUsageLogRepository.findAll(spec, pageable);
    }

    public Optional<TokenUsageLog> findById(Long id) {
        return tokenUsageLogRepository.findById(id);
    }
}
