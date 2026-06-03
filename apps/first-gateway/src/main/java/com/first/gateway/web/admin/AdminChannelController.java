package com.first.gateway.web.admin;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.channel.ChannelService;
import com.first.gateway.service.channel.ChannelTestService;
import com.first.gateway.web.admin.dto.ChannelRequest;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/channels")
public class AdminChannelController {

    private final ChannelService channelService;
    private final ChannelTestService channelTestService;

    public AdminChannelController(ChannelService channelService, ChannelTestService channelTestService) {
        this.channelService = channelService;
        this.channelTestService = channelTestService;
    }

    @GetMapping
    public List<Channel> list(HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return channelService.listAll();
    }

    @GetMapping("/{id}")
    public Channel get(@PathVariable Long id, HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        return channelService.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Channel not found"));
    }

    @PostMapping
    public Channel create(@RequestBody ChannelRequest request, HttpServletRequest httpRequest) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(httpRequest)));
        return channelService.createFromRequest(request);
    }

    @PutMapping("/{id}")
    public Channel update(@PathVariable Long id,
                          @RequestBody ChannelRequest request,
                          HttpServletRequest httpRequest) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(httpRequest)));
        return channelService.updateFromRequest(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        channelService.delete(id);
        return Map.of("message", "ok");
    }

    @PostMapping("/{id}/test")
    public Map<String, Object> test(@PathVariable Long id, HttpServletRequest request) {
        AdminAccess.requireAdmin(AdminAccess.requirePrincipal(principal(request)));
        Channel channel = channelService.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Channel not found"));
        return channelTestService.test(channel);
    }

    private static AdminPrincipal principal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        return value instanceof AdminPrincipal p ? p : null;
    }
}
