package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.McpServer;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.mcp.McpServerService;
import com.first.gateway.web.workspace.dto.McpServerRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mcp-servers")
public class WorkspaceMcpController {

    private final McpServerService mcpServerService;

    public WorkspaceMcpController(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @GetMapping
    public List<McpServer> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return mcpServerService.list(principal.userId());
    }

    @GetMapping("/{id}")
    public McpServer get(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return mcpServerService.require(id, principal.userId());
    }

    @PostMapping
    public McpServer create(@RequestBody McpServerRequest body, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return mcpServerService.create(principal.tenantId(), principal.userId(),
            body.name(), body.endpoint(), body.transport());
    }

    @PutMapping("/{id}")
    public McpServer update(@PathVariable Long id,
                            @RequestBody McpServerRequest body,
                            HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return mcpServerService.update(id, principal.userId(), body.name(), body.endpoint());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        mcpServerService.delete(id, principal.userId());
        return Map.of("message", "ok");
    }

    @PostMapping("/{id}/test")
    public Map<String, Object> test(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return mcpServerService.test(id, principal.userId());
    }

    @PutMapping("/{id}/toggle")
    public McpServer toggle(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return mcpServerService.toggle(id, principal.userId());
    }
}