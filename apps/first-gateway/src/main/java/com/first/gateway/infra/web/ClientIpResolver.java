package com.first.gateway.infra.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public final class ClientIpResolver {

    private static final Set<String> TRUSTED_PROXIES = Set.of(
        "127.0.0.1", "0:0:0:0:0:0:0:1"
    );

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }

    private static boolean isTrustedProxy(String ip) {
        return TRUSTED_PROXIES.contains(ip);
    }
}
