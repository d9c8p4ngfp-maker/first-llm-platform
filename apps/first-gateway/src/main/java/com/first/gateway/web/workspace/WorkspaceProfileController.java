package com.first.gateway.web.workspace;

import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.profile.ProfileStatusService;
import com.first.gateway.service.profile.UserProfileService;
import com.first.gateway.web.workspace.dto.ProfileStatusResponse;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
public class WorkspaceProfileController {

    private final ProfileStatusService profileStatusService;
    private final UserProfileService userProfileService;

    public WorkspaceProfileController(ProfileStatusService profileStatusService,
                                       UserProfileService userProfileService) {
        this.profileStatusService = profileStatusService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/status")
    public ProfileStatusResponse getStatus(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return profileStatusService.getStatus(principal.userId());
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return userProfileService.profileSummary(principal.userId(), principal.tenantId(), principal.username());
    }

    @PutMapping("/status")
    public ProfileStatusResponse updateStatus(@RequestBody Map<String, Boolean> body,
                                               HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return profileStatusService.updateStatus(
            principal.userId(),
            body.getOrDefault("memoryEnabled", true),
            body.getOrDefault("profileEnabled", true),
            body.getOrDefault("profileInChat", true));
    }
}
