package com.first.gateway.infra.filter;

public final class GatewayRequestAttributes {

    public static final String AUTH_CONTEXT = "gateway.authContext";
    public static final String ADMIN_PRINCIPAL = "gateway.adminPrincipal";
    public static final String RATE_LIMIT_CHECKOUT = "gateway.rateLimitCheckout";
    public static final String TPM_RESERVED = "gateway.tpmReserved";

    private GatewayRequestAttributes() {}
}