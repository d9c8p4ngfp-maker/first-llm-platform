package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.profile.UserMemoryService;
import com.first.gateway.web.workspace.dto.UserMemoryRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user-memories")
public class WorkspaceMemoryController {

    private final UserMemoryService userMemoryService;

    public WorkspaceMemoryController(UserMemoryService userMemoryService) {
        this.userMemoryService = userMemoryService;
    }

    @GetMapping
    public List<UserMemory> list(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String status,
                                 HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return userMemoryService.list(principal.userId(), category, status);
    }

    @PostMapping
    public UserMemory create(@RequestBody UserMemoryRequest body, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return userMemoryService.create(principal.userId(), principal.tenantId(),
            body.category(), body.content(), body.importance(), body.scheduleDate(),
            body.scheduleTime(), body.numericValue(), null);
    }

    @PutMapping("/{id}")
    public UserMemory update(@PathVariable Long id,
                             @RequestBody UserMemoryRequest body,
                             HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return userMemoryService.update(id, principal.userId(), body.content(), body.importance(),
            body.scheduleDate(), body.scheduleTime(), body.numericValue());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        userMemoryService.delete(id, principal.userId());
        return Map.of("message", "ok");
    }

    @PutMapping("/{id}/done")
    public UserMemory done(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return userMemoryService.markDone(id, principal.userId());
    }

    @PutMapping("/{id}/archive")
    public UserMemory archive(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return userMemoryService.archive(id, principal.userId());
    }
}