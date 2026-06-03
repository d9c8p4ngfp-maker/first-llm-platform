package com.first.gateway.infra.error;

public class RateLimitExceededException extends GatewayException {

    public enum LimitType {
        RPM, TPM, CONCURRENT
    }

    private final LimitType limitType;
    private final int retryAfterSeconds;
    private final Integer limit;
    private final Integer remaining;

    public RateLimitExceededException(LimitType limitType, int retryAfterSeconds, Integer limit, Integer remaining) {
        super(GatewayError.RATE_LIMIT_EXCEEDED);
        this.limitType = limitType;
        this.retryAfterSeconds = retryAfterSeconds;
        this.limit = limit;
        this.remaining = remaining;
    }

    public LimitType getLimitType() {
        return limitType;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getRemaining() {
        return remaining;
    }
}