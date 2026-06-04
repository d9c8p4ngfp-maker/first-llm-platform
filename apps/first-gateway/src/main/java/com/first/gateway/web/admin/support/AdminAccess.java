package com.first.gateway.web.admin.support;

import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.UserTenantRelRepository;
import com.first.gateway.service.auth.admin.AdminPrincipal;

public final class AdminAccess {

    private AdminAccess() {}

    public static AdminPrincipal requirePrincipal(AdminPrincipal principal) {
        if (principal == null) {
            throw new GatewayException(GatewayError.INVALID_JWT);
        }
        return principal;
    }

    public static void requireAdmin(AdminPrincipal principal) {
        requirePrincipal(principal);
        if (!principal.isAdmin()) {
            throw new GatewayException(GatewayError.ACCESS_DENIED);
        }
    }

    public static AdminPrincipal requirePlatformAdmin(AdminPrincipal principal) {
        requirePrincipal(principal);
        if (!principal.isPlatformAdmin()) {
            throw new GatewayException(GatewayError.ACCESS_DENIED, "Platform admin required");
        }
        return principal;
    }

    public static void requireSelfOrAdmin(AdminPrincipal principal, Long userId) {
        requirePrincipal(principal);
        if (principal.userId().equals(userId)) {
            return;
        }
        if (!principal.isPlatformAdmin()) {
            throw new GatewayException(GatewayError.ACCESS_DENIED);
        }
    }

    public static void requireSelfOrAdminInTenant(AdminPrincipal principal, Long userId,
                                                  UserTenantRelRepository relRepository) {
        requirePrincipal(principal);
        if (principal.userId().equals(userId)) {
            return;
        }
        requirePlatformAdmin(principal);
        relRepository.findByUserIdAndTenantId(userId, principal.tenantId())
            .orElseThrow(() -> new GatewayException(GatewayError.ACCESS_DENIED));
    }
}