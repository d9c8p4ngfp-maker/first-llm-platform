package com.first.gateway.web.workspace;

import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.dashboard.DashboardService;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class WorkspaceDashboardController {

    private final DashboardService dashboardService;

    public WorkspaceDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/realtime")
    public Map<String, Object> realtime(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return dashboardService.realtime(principal.tenantId(), principal.userId(), principal.username());
    }
}