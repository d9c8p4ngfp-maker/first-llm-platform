package com.first.gateway.web.admin;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.channel.ChannelService;
import com.first.gateway.service.channel.ChannelTestService;
import com.first.gateway.web.admin.dto.ChannelRequest;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/channels")
public class AdminChannelController {

    private final ChannelService channelService;
    private final ChannelTestService channelTestService;
    private final ChannelRepository channelRepository;

    public AdminChannelController(ChannelService channelService,
                                  ChannelTestService channelTestService,
                                  ChannelRepository channelRepository) {
        this.channelService = channelService;
        this.channelTestService = channelTestService;
        this.channelRepository = channelRepository;
    }

    @GetMapping
    public List<Channel> list(HttpServletRequest request) {
        AdminPrincipal p = AdminAccess.requirePlatformAdmin(AdminAccess.requirePrincipal(principal(request)));
        return channelRepository.findByTenantIdAndDeleted(p.tenantId(), (short) 0);
    }

    @GetMapping("/{id}")
    public Channel get(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal p = AdminAccess.requirePlatformAdmin(AdminAccess.requirePrincipal(principal(request)));
        return channelService.requireInTenant(id, p.tenantId());
    }

    @PostMapping
    public Channel create(@Validated(ChannelRequest.OnCreate.class) @RequestBody ChannelRequest body,
                          HttpServletRequest httpRequest) {
        AdminPrincipal p = AdminAccess.requirePlatformAdmin(AdminAccess.requirePrincipal(principal(httpRequest)));
        return channelService.createFromRequest(body, p.tenantId());
    }

    @PutMapping("/{id}")
    public Channel update(@PathVariable Long id,
                          @Valid @RequestBody ChannelRequest body,
                          HttpServletRequest httpRequest) {
        AdminPrincipal p = AdminAccess.requirePlatformAdmin(AdminAccess.requirePrincipal(principal(httpRequest)));
        channelService.requireInTenant(id, p.tenantId());
        return channelService.updateFromRequest(id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal p = AdminAccess.requirePlatformAdmin(AdminAccess.requirePrincipal(principal(request)));
        channelService.requireInTenant(id, p.tenantId());
        channelService.delete(id);
        return Map.of("message", "ok");
    }

    @PostMapping("/{id}/test")
    public Map<String, Object> test(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal p = AdminAccess.requirePlatformAdmin(AdminAccess.requirePrincipal(principal(request)));
        Channel channel = channelService.requireInTenant(id, p.tenantId());
        return channelTestService.test(channel);
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
