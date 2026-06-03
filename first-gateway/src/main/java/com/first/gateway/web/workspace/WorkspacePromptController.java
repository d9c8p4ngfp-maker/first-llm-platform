package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.PromptTemplate;
import com.first.gateway.domain.entity.PromptVersion;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.prompt.PromptTemplateService;
import com.first.gateway.web.workspace.dto.PromptPreviewRequest;
import com.first.gateway.web.workspace.dto.PromptTemplateRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prompt-templates")
public class WorkspacePromptController {

    private final PromptTemplateService promptTemplateService;

    public WorkspacePromptController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping
    public List<PromptTemplate> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.list(principal.tenantId());
    }

    @GetMapping("/favorites")
    public List<PromptTemplate> favorites(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.favorites(principal.userId());
    }

    @GetMapping("/{id}")
    public PromptTemplate get(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.require(id, principal.tenantId());
    }

    @PostMapping
    public PromptTemplate create(@RequestBody PromptTemplateRequest body, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.create(
            principal.tenantId(), principal.userId(),
            body.name(), body.description(), body.industry(), body.category(), body.visibility(),
            body.systemPrompt(), body.userPromptTemplate(), body.variables(), body.suggestedModel());
    }

    @PutMapping("/{id}")
    public PromptTemplate update(@PathVariable Long id,
                                 @RequestBody PromptTemplateRequest body,
                                 HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.update(
            id, principal.tenantId(), principal.userId(),
            body.name(), body.description(), body.industry(), body.category(), body.visibility(),
            body.systemPrompt(), body.userPromptTemplate(), body.variables(), body.suggestedModel());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        promptTemplateService.delete(id, principal.tenantId());
        return Map.of("message", "ok");
    }

    @GetMapping("/{id}/versions")
    public List<PromptVersion> versions(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.versions(id, principal.tenantId());
    }

    @PostMapping("/{id}/favorite")
    public Map<String, Object> favorite(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.toggleFavorite(id, principal.userId(), principal.tenantId());
    }

    @PostMapping("/{id}/preview")
    public Map<String, Object> preview(@PathVariable Long id,
                                       @RequestBody PromptPreviewRequest body,
                                       HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.preview(id, principal.tenantId(),
            body != null ? body.variables() : null);
    }

    @PostMapping("/{id}/versions/{versionId}/rollback")
    public PromptTemplate rollback(@PathVariable Long id, @PathVariable Long versionId,
                                    HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return promptTemplateService.rollback(id, principal.tenantId(), versionId);
    }
}