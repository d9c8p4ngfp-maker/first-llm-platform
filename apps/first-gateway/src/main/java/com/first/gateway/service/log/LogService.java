package com.first.gateway.service.log;

import com.first.gateway.domain.entity.TokenUsageLog;

public interface LogService {

    TokenUsageLog save(TokenUsageLog log);
}
