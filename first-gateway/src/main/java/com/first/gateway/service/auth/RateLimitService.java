package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.RateLimitExceededException;
import com.first.gateway.service.monitor.MonitoringService;
import org.springframework.stereotype.Component;

@Component
public class RateLimitService {

    private final ApiKeyRateLimiter apiKeyRateLimiter;
    private final TpmRateLimiter tpmRateLimiter;
    private final ConcurrencyLimiter concurrencyLimiter;
    private final MonitoringService monitoringService;

    public RateLimitService(ApiKeyRateLimiter apiKeyRateLimiter,
                            TpmRateLimiter tpmRateLimiter,
                            ConcurrencyLimiter concurrencyLimiter,
                            MonitoringService monitoringService) {
        this.apiKeyRateLimiter = apiKeyRateLimiter;
        this.tpmRateLimiter = tpmRateLimiter;
        this.concurrencyLimiter = concurrencyLimiter;
        this.monitoringService = monitoringService;
    }

    public RateLimitCheckout acquire(ApiKey apiKey) {
        apiKeyRateLimiter.check(apiKey);
        String slotId = concurrencyLimiter.acquire(apiKey);
        return new RateLimitCheckout(slotId, 0);
    }

    public long reserveTpm(ApiKey apiKey, long tokensNeeded) {
        return tpmRateLimiter.reserve(apiKey, tokensNeeded);
    }

    public void releaseConcurrency(ApiKey apiKey, RateLimitCheckout checkout) {
        if (checkout != null) {
            concurrencyLimiter.release(apiKey, checkout.concurrencySlotId());
        }
    }

    public void recordRateLimit(RateLimitExceededException ex) {
        monitoringService.recordRateLimit(ex.getLimitType());
    }
}
