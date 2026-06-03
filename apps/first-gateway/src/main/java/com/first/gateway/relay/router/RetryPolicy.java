package com.first.gateway.relay.router;

import com.first.gateway.config.GatewayProperties;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import org.springframework.stereotype.Component;

@Component
public class RetryPolicy {

    private final GatewayProperties gatewayProperties;

    public RetryPolicy(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    public int maxAttempts() {
        return gatewayProperties.getRetry().getMaxAttempts();
    }

    public boolean shouldRetry(int attemptIndex, Throwable error) {
        if (attemptIndex >= maxAttempts()) {
            return false;
        }
        if (!(error instanceof GatewayException gatewayException)) {
            return false;
        }
        GatewayError code = gatewayException.getError();
        return code == GatewayError.UPSTREAM_ERROR
            || code == GatewayError.UPSTREAM_TIMEOUT
            || code == GatewayError.RATE_LIMIT_EXCEEDED;
    }
}
