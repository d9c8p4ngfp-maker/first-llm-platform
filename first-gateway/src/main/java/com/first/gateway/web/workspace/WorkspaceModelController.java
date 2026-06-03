package com.first.gateway.web.workspace;

import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.console.ConsolePreferenceService;
import com.first.gateway.web.workspace.dto.ModelPreferencesRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/models")
public class WorkspaceModelController {

    private final ConsolePreferenceService preferenceService;

    public WorkspaceModelController(ConsolePreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public List<Map<String, Object>> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return preferenceService.listModelsForUser(principal.userId());
    }

    @GetMapping("/preferences")
    public Map<String, Object> preferences(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return preferenceService.getModelPreferences(principal.userId());
    }

    @PutMapping("/preferences")
    public Map<String, Object> savePreferences(@RequestBody ModelPreferencesRequest body,
                                               HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return preferenceService.saveModelPreferences(principal.userId(), body.defaultModel());
    }
}