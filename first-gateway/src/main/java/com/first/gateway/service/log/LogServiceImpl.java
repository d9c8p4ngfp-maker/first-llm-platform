package com.first.gateway.service.log;

import com.first.gateway.domain.entity.TokenUsageLog;
import com.first.gateway.repository.TokenUsageLogRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogServiceImpl implements LogService {

    private final TokenUsageLogRepository tokenUsageLogRepository;

    public LogServiceImpl(TokenUsageLogRepository tokenUsageLogRepository) {
        this.tokenUsageLogRepository = tokenUsageLogRepository;
    }

    @Override
    public TokenUsageLog save(TokenUsageLog log) {
        if (log.getRequestId() == null) {
            log.setRequestId(MDC.get("traceId"));
        }
        return tokenUsageLogRepository.save(log);
    }
}
