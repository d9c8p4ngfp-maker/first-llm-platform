package com.first.gateway.web.workspace.support;

import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.service.auth.admin.AdminPrincipal;

public final class WorkspaceAccess {

    private WorkspaceAccess() {}

    public static AdminPrincipal requirePrincipal(AdminPrincipal principal) {
        if (principal == null) {
            throw new GatewayException(GatewayError.INVALID_JWT);
        }
        return principal;
    }
}
