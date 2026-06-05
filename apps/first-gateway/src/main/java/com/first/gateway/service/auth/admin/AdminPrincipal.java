package com.first.gateway.service.auth.admin;

import com.first.gateway.domain.enums.TenantRole;

public record AdminPrincipal(Long userId, Long tenantId, String username, TenantRole role) {

    public boolean isPlatformAdmin() {
        return TenantRole.PLATFORM_ADMIN == role;
    }

    public boolean isAdmin() {
        return isPlatformAdmin();
    }
}