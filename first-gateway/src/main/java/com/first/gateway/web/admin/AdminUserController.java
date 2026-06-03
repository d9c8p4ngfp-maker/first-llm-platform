package com.first.gateway.web.admin;

import com.first.gateway.domain.entity.Quota;
import com.first.gateway.domain.entity.User;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.repository.UserRepository;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.user.UserService;
import com.first.gateway.web.admin.dto.AdjustQuotaRequest;
import com.first.gateway.web.admin.dto.CreateUserRequest;
import com.first.gateway.web.admin.dto.UpdateUserRequest;
import com.first.gateway.web.admin.dto.UpdateUserStatusRequest;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public AdminUserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping
    public List<User> list(HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(principal(request));
        AdminAccess.requireSelfOrAdmin(principal, id);
        return userRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
    }

    @PostMapping
    public User create(@Valid @RequestBody CreateUserRequest body, HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return userService.createUser(body.username(), body.password(), body.email(), body.groupId());
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id,
                       @RequestBody UpdateUserRequest body,
                       HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return userService.updateUser(id, body.email(), body.groupId());
    }

    @PutMapping("/{id}/status")
    public User updateStatus(@PathVariable Long id,
                             @Valid @RequestBody UpdateUserStatusRequest body,
                             HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return userService.updateStatus(id, body.status());
    }

    @PutMapping("/{id}/quota")
    public Map<String, Object> adjustQuota(@PathVariable Long id,
                                           @Valid @RequestBody AdjustQuotaRequest body,
                                           HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        Quota quota = userService.adjustUserQuota(id, body.totalTokens());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", quota.getTenantId());
        result.put("totalTokens", quota.getTotalTokens());
        result.put("usedTokens", quota.getUsedTokens());
        return result;
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
