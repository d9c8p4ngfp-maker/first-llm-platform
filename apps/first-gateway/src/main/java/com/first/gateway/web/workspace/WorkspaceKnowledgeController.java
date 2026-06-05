package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.KnowledgeBase;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.knowledge.FileStorageService;
import com.first.gateway.service.knowledge.KnowledgeBaseService;
import com.first.gateway.web.workspace.dto.KnowledgeBaseRequest;
import com.first.gateway.web.workspace.dto.KnowledgeDocumentRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class WorkspaceKnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final FileStorageService fileStorageService;

    public WorkspaceKnowledgeController(KnowledgeBaseService knowledgeBaseService,
                                         FileStorageService fileStorageService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<KnowledgeBase> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return knowledgeBaseService.listByTenant(principal.tenantId());
    }

    @PostMapping
    public KnowledgeBase create(@Valid @RequestBody KnowledgeBaseRequest body, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return knowledgeBaseService.create(principal.tenantId(), body.name(), body.description());
    }

    @GetMapping("/{id}")
    public KnowledgeBase get(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return knowledgeBaseService.requireByTenant(id, principal.tenantId());
    }

    @PutMapping("/{id}")
    public KnowledgeBase update(@PathVariable Long id,
                                @Valid @RequestBody KnowledgeBaseRequest body,
                                HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return knowledgeBaseService.update(id, principal.tenantId(), body.name(), body.description());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        knowledgeBaseService.delete(id, principal.tenantId());
        return Map.of("message", "ok");
    }

    @GetMapping("/{id}/documents")
    public List<KnowledgeDocument> documents(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return knowledgeBaseService.listDocuments(id, principal.tenantId());
    }

    @PostMapping("/{id}/documents")
    public KnowledgeDocument createDocument(@PathVariable Long id,
                                         @Valid @RequestBody KnowledgeDocumentRequest body,
                                         HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return knowledgeBaseService.createDocument(id, principal.tenantId(), principal.userId(), body.title(), body.content());
    }

    @DeleteMapping("/{id}/documents/{docId}")
    public Map<String, String> deleteDocument(@PathVariable Long id,
                                              @PathVariable Long docId,
                                              HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        knowledgeBaseService.deleteDocument(id, principal.tenantId(), docId);
        return Map.of("message", "ok");
    }

    @PostMapping("/{id}/documents/{docId}/reindex")
    public KnowledgeDocument reindexDocument(@PathVariable Long id,
                                             @PathVariable Long docId,
                                             HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return knowledgeBaseService.reindexDocument(id, principal.tenantId(), principal.userId(), docId);
    }

    @PostMapping("/{id}/search")
    public List<Map<String, Object>> search(@PathVariable Long id,
                                             @RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        String query = body.get("query") != null ? body.get("query").toString() : "";
        int topK = body.get("top_k") instanceof Number n ? n.intValue() : 5;
        return knowledgeBaseService.search(id, principal.tenantId(), query, topK);
    }

    @PostMapping(value = "/{id}/documents/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public KnowledgeDocument uploadDocument(@PathVariable Long id,
                                             @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                             @RequestParam(value = "title", required = false) String title,
                                             HttpServletRequest request) throws java.io.IOException {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        String docTitle = title != null ? title : file.getOriginalFilename();
        FileStorageService.FileStorageResult stored = fileStorageService.store(file, id);
        return knowledgeBaseService.createDocumentFromFile(id, principal.tenantId(), principal.userId(),
            docTitle, stored);
    }
}