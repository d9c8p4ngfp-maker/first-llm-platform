package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.token.TokenService;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tokens")
public class WorkspaceTokenController {

    private final TokenService tokenService;

    public WorkspaceTokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping
    public List<ApiKey> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return tokenService.listByUserId(principal.userId());
    }

    @PostMapping
    public Map<String, Object> create(@RequestParam(required = false) String name,
                                      HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        TokenService.CreatedToken created = tokenService.create(
            principal.tenantId(), principal.userId(), name);
        return Map.of(
            "key", created.rawKey(),
            "apiKey", created.apiKey()
        );
    }

    @DeleteMapping("/{id}")
    public Map<String, String> revoke(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        ApiKey apiKey = tokenService.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("token not found"));
        if (!principal.userId().equals(apiKey.getUserId())) {
            throw new com.first.gateway.infra.error.GatewayException(
                com.first.gateway.infra.error.GatewayError.ACCESS_DENIED);
        }
        tokenService.revoke(id);
        return Map.of("message", "ok");
    }
}
