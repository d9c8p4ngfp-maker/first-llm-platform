package com.first.gateway.web.admin;

import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.system.SystemConfigService;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final SystemConfigService systemConfigService;

    public AdminSettingsController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public Map<String, String> get(HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return systemConfigService.allSettings();
    }

    @PutMapping
    public Map<String, String> update(@RequestBody Map<String, String> body, HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return systemConfigService.updateSettings(body);
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
