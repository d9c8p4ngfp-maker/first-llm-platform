package com.first.gateway.web.admin.support;

import com.first.gateway.domain.enums.TenantRole;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminAccessTest {

    @Test
    void requireAdmin_allowsPlatformAdmin() {
        assertDoesNotThrow(() -> AdminAccess.requireAdmin(new AdminPrincipal(1L, 1L, "admin", TenantRole.PLATFORM_ADMIN)));
    }

    @Test
    void requireAdmin_rejectsMember() {
        GatewayException ex = assertThrows(GatewayException.class,
            () -> AdminAccess.requireAdmin(new AdminPrincipal(2L, 1L, "user", TenantRole.MEMBER)));
        assertEquals(GatewayError.ACCESS_DENIED, ex.getError());
    }

    @Test
    void requireSelfOrAdmin_allowsSelf() {
        AdminPrincipal member = new AdminPrincipal(2L, 1L, "user", TenantRole.MEMBER);
        assertDoesNotThrow(() -> AdminAccess.requireSelfOrAdmin(member, 2L));
    }
}
