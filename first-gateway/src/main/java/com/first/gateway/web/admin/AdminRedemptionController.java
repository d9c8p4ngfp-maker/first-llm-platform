package com.first.gateway.web.admin;

import com.first.gateway.domain.entity.RedemptionCode;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.billing.RedemptionService;
import com.first.gateway.web.admin.dto.BatchRedemptionRequest;
import com.first.gateway.web.admin.dto.RedeemCodeRequest;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/redemption-codes")
public class AdminRedemptionController {

    private final RedemptionService redemptionService;

    public AdminRedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @GetMapping
    public List<RedemptionCode> list(HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return redemptionService.listAll();
    }

    @PostMapping("/batch")
    public List<RedemptionCode> batch(@Valid @RequestBody BatchRedemptionRequest body,
                                      HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return redemptionService.batchCreate(body.count(), body.amount(), body.expiresAt());
    }

    @PostMapping("/redeem")
    public RedemptionCode redeem(@Valid @RequestBody RedeemCodeRequest body,
                                 HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(principal(request));
        return redemptionService.redeem(principal.userId(), body.code());
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
