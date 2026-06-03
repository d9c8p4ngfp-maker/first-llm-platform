package com.first.gateway.web.admin;

import com.first.gateway.domain.entity.TokenUsageLog;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.log.LogQueryService;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {

    private final LogQueryService logQueryService;

    public AdminLogController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @GetMapping
    public Page<TokenUsageLog> list(@RequestParam(required = false) Long tenantId,
                                    @RequestParam(required = false) Long apiKeyId,
                                    @RequestParam(required = false) String model,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(principal(request));
        if (!principal.isAdmin()) {
            tenantId = principal.tenantId();
        }
        return logQueryService.search(tenantId, apiKeyId, model, status, startDate, endDate,
            PageRequest.of(page, size));
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
