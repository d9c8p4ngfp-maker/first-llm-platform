package com.first.gateway.web.workspace;

import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.profile.UserProfileService;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user-profiles")
public class WorkspaceProfileController {

    private final UserProfileService userProfileService;

    public WorkspaceProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return userProfileService.getProfile(principal.userId(), principal.tenantId(), principal.username());
    }

    @PostMapping("/me/refresh")
    public Map<String, Object> refresh(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return userProfileService.refresh(principal.userId(), principal.tenantId());
    }

    @DeleteMapping("/me")
    public Map<String, String> clear(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        userProfileService.clearAll(principal.userId(), principal.tenantId());
        return Map.of("message", "ok");
    }
}
