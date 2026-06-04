package com.first.gateway.service.auth.admin;

public record AdminPrincipal(Long userId, Long tenantId, String username, String role) {

    public boolean isPlatformAdmin() {
        return "PLATFORM_ADMIN".equals(role);
    }

    public boolean isTenantOwner() {
        return "OWNER".equals(role);
    }

    public boolean isAdmin() {
        return isPlatformAdmin();
    }
}