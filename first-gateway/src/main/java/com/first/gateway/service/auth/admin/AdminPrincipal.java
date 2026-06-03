package com.first.gateway.service.auth.admin;

public record AdminPrincipal(Long userId, Long tenantId, String username, String role) {

    public boolean isAdmin() {
        return "OWNER".equals(role) || "ADMIN".equals(role);
    }
}