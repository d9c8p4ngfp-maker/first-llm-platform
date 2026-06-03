package com.first.gateway.web.admin;

import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.log.StatsService;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/daily")
    public List<StatsService.DailyStat> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String model,
            HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(principal(request));
        if (!principal.isAdmin()) {
            tenantId = principal.tenantId();
        }
        return statsService.dailyStats(startDate, endDate, tenantId, model);
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
