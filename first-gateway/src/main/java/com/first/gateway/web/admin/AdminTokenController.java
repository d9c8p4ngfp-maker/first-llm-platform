package com.first.gateway.web.admin;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.User;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.channel.ChannelService;
import com.first.gateway.service.token.TokenService;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tokens")
public class AdminTokenController {

    private final TokenService tokenService;

    public AdminTokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping
    public List<ApiKey> list(@RequestParam Long userId, HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(principal(request));
        AdminAccess.requireSelfOrAdmin(principal, userId);
        return tokenService.listByUserId(userId);
    }

    @PostMapping
    public Map<String, Object> create(@RequestParam Long tenantId,
                                      @RequestParam Long userId,
                                      @RequestParam(required = false) String name,
                                      HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(principal(request));
        AdminAccess.requireSelfOrAdmin(principal, userId);
        if (!principal.isAdmin() && !tenantId.equals(principal.tenantId())) {
            throw new GatewayException(GatewayError.ACCESS_DENIED);
        }
        TokenService.CreatedToken created = tokenService.create(tenantId, userId, name);
        return Map.of(
            "key", created.rawKey(),
            "apiKey", created.apiKey()
        );
    }

    @DeleteMapping("/{id}")
    public Map<String, String> revoke(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(principal(request));
        ApiKey apiKey = tokenService.findById(id)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "token not found"));
        AdminAccess.requireSelfOrAdmin(principal, apiKey.getUserId());
        tokenService.revoke(id);
        return Map.of("message", "ok");
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
