package com.first.gateway.web.workspace;

import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.console.ConsolePreferenceService;
import com.first.gateway.web.workspace.dto.SettingsUpdateRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
public class WorkspaceSettingsController {

    private final ConsolePreferenceService preferenceService;

    public WorkspaceSettingsController(ConsolePreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public Map<String, Object> get(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return preferenceService.getSettings(principal.userId());
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody SettingsUpdateRequest body, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return preferenceService.saveSettings(principal.userId(), body.defaultModel(), body.theme(), body.language());
    }
}