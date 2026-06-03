package com.first.gateway.web.workspace;

import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.log.StatsService;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
public class WorkspaceStatsController {

    private final StatsService statsService;

    public WorkspaceStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/daily")
    public List<StatsService.DailyStat> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String model,
            HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return statsService.dailyStats(dateFrom, dateTo, principal.tenantId(), model);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return statsService.summary(principal.tenantId());
    }
}
