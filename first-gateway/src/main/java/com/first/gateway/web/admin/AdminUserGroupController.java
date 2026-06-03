package com.first.gateway.web.admin;

import com.first.gateway.domain.entity.UserGroup;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.user.UserGroupService;
import com.first.gateway.web.admin.dto.UserGroupRequest;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/user-groups")
public class AdminUserGroupController {

    private final UserGroupService userGroupService;

    public AdminUserGroupController(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }

    @GetMapping
    public List<UserGroup> list(HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return userGroupService.listAll();
    }

    @PostMapping
    public UserGroup create(@RequestBody UserGroupRequest body, HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return userGroupService.create(body.name(), body.ratio());
    }

    @PutMapping("/{id}")
    public UserGroup update(@PathVariable Long id,
                            @RequestBody UserGroupRequest body,
                            HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return userGroupService.update(id, body.name(), body.ratio());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        userGroupService.delete(id);
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
