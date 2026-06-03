package com.first.gateway.web.workspace.support;

import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;

public final class WorkspaceRequest {

    private WorkspaceRequest() {}

    public static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
