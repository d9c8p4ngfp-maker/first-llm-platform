package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.PipelineConfig;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.pipeline.PipelineConfigService;
import com.first.gateway.web.workspace.dto.PipelineOverrideRequest;
import com.first.gateway.web.workspace.dto.PipelinePreviewRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pipeline-configs")
public class WorkspacePipelineController {

    private final PipelineConfigService pipelineConfigService;

    public WorkspacePipelineController(PipelineConfigService pipelineConfigService) {
        this.pipelineConfigService = pipelineConfigService;
    }

    @GetMapping
    public List<PipelineConfig> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return pipelineConfigService.listForUser(principal.userId());
    }

    @GetMapping("/{configKey}")
    public PipelineConfig get(@PathVariable String configKey, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return pipelineConfigService.getByKey(configKey, principal.userId());
    }

    @PutMapping("/{configKey}/override")
    public PipelineConfig override(@PathVariable String configKey,
                                   @RequestBody PipelineOverrideRequest body,
                                   HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return pipelineConfigService.saveOverride(principal.userId(), configKey,
            body.modelId(), body.modelParams(), body.promptTemplateId(),
            body.promptText(), body.enabled(), null);
    }

    @DeleteMapping("/{configKey}/override")
    public Map<String, String> resetOverride(@PathVariable String configKey, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        pipelineConfigService.deleteOverride(configKey, principal.userId());
        return Map.of("message", "ok");
    }

    @PostMapping("/{configKey}/preview")
    public Map<String, Object> preview(@PathVariable String configKey,
                                       @RequestBody PipelinePreviewRequest body,
                                       HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        java.util.Map<String, String> variables = null;
        if (body != null && body.variables() != null) {
            variables = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, Object> entry : body.variables().entrySet()) {
                variables.put(entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }
        }
        return pipelineConfigService.preview(configKey, principal.userId(), variables);
    }
}