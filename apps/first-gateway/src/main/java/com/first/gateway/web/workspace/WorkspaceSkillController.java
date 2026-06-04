package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.Skill;
import com.first.gateway.domain.entity.SkillBinding;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.skill.SkillService;
import com.first.gateway.web.workspace.dto.SkillBindingRequest;
import com.first.gateway.web.workspace.dto.SkillRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/skills")
public class WorkspaceSkillController {

    private final SkillService skillService;

    public WorkspaceSkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public List<Skill> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return skillService.list(principal.userId());
    }

    @GetMapping("/{id}")
    public Skill get(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return skillService.require(id, principal.userId());
    }

    @PostMapping
    public Skill create(@Valid @RequestBody SkillRequest body, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return skillService.create(principal.tenantId(), principal.userId(),
            body.name(), body.description(), body.suggestedModel());
    }

    @PutMapping("/{id}")
    public Skill update(@PathVariable Long id,
                        @Valid @RequestBody SkillRequest body,
                        HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return skillService.update(id, principal.userId(),
            body.name(), body.description(), body.suggestedModel());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        skillService.delete(id, principal.userId());
        return Map.of("message", "ok");
    }

    @PutMapping("/{id}/toggle")
    public Skill toggle(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return skillService.toggle(id, principal.userId());
    }

    @PostMapping("/{id}/bindings")
    public SkillBinding addBinding(@PathVariable Long id,
                                  @Valid @RequestBody SkillBindingRequest body,
                                  HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return skillService.addBinding(id, principal.userId(), body.type(), body.bindingId());
    }

    @DeleteMapping("/{id}/bindings/{bindingId}")
    public Map<String, String> removeBinding(@PathVariable Long id,
                                             @PathVariable Long bindingId,
                                             HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        skillService.removeBinding(id, principal.userId(), bindingId);
        return Map.of("message", "ok");
    }
}