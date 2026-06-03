package com.first.gateway.web.admin;

import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminAuthService;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.web.admin.dto.LoginRequest;
import com.first.gateway.web.admin.dto.RegisterRequest;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/admin/api/v1/auth"})
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        AdminAuthService.LoginResult result = adminAuthService.login(
            request.username(), request.password());
        return loginResponse(result);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        AdminAuthService.LoginResult result = adminAuthService.register(
            request.username(), request.password(), request.email());
        return loginResponse(result);
    }

    @GetMapping("/register-enabled")
    public Map<String, Object> registerEnabled() {
        return Map.of("enabled", adminAuthService.isRegisterEnabled());
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(currentPrincipal(request));
        return userResponse(principal);
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("message", "ok");
    }

    private static AdminPrincipal currentPrincipal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        if (value instanceof AdminPrincipal principal) {
            return principal;
        }
        return null;
    }

    private static Map<String, Object> loginResponse(AdminAuthService.LoginResult result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", result.accessToken());
        body.put("token_type", "Bearer");
        body.put("expires_in", result.expiresIn());
        body.put("user", userResponse(result.principal()));
        return body;
    }

    private static Map<String, Object> userResponse(AdminPrincipal principal) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", principal.userId());
        user.put("username", principal.username());
        user.put("tenant_id", principal.tenantId());
        user.put("role", principal.role());
        return user;
    }
}