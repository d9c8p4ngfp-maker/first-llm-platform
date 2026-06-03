package com.first.gateway.service.auth;

public record RateLimitCheckout(String concurrencySlotId, long tpmReserved) {
}
