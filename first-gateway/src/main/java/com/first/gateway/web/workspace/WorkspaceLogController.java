package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.TokenUsageLog;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.log.LogQueryService;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/logs")
public class WorkspaceLogController {

    private final LogQueryService logQueryService;

    public WorkspaceLogController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @GetMapping
    public Page<TokenUsageLog> list(@RequestParam(required = false) Long apiKeyId,
                                    @RequestParam(required = false) String model,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return logQueryService.search(
            principal.tenantId(), apiKeyId, model, status, dateFrom, dateTo, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public TokenUsageLog get(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        TokenUsageLog log = logQueryService.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("log not found"));
        if (!principal.tenantId().equals(log.getTenantId())) {
            throw new com.first.gateway.infra.error.GatewayException(
                com.first.gateway.infra.error.GatewayError.ACCESS_DENIED);
        }
        return log;
    }
}
