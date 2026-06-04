package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.Conversation;
import com.first.gateway.domain.entity.ConversationMessage;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.conversation.ConversationService;
import com.first.gateway.web.workspace.dto.ConversationMessageRequest;
import com.first.gateway.web.workspace.dto.ConversationUpdateRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
public class WorkspaceConversationController {

    private final ConversationService conversationService;

    public WorkspaceConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<Conversation> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return conversationService.listByTenant(principal.tenantId());
    }

    @PostMapping
    public Conversation create(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return conversationService.create(principal.tenantId());
    }

    @GetMapping("/{id}/messages")
    public List<ConversationMessage> messages(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return conversationService.listMessages(id, principal.tenantId());
    }

    @PostMapping("/{id}/messages")
    public ConversationMessage appendMessage(@PathVariable Long id,
                                             @Valid @RequestBody ConversationMessageRequest body,
                                             HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return conversationService.appendMessage(
            id, principal.tenantId(), principal.userId(), body.role(), body.content());
    }

    @PutMapping("/{id}")
    public Conversation update(@PathVariable Long id,
                               @Valid @RequestBody ConversationUpdateRequest body,
                               HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return conversationService.rename(id, principal.tenantId(), body.title());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        conversationService.delete(id, principal.tenantId());
        return Map.of("message", "ok");
    }
}