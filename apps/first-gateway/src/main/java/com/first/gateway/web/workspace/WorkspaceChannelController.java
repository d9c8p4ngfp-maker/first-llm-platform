package com.first.gateway.web.workspace;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.channel.ChannelService;
import com.first.gateway.service.channel.ChannelTestService;
import com.first.gateway.web.admin.dto.ChannelRequest;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels")
public class WorkspaceChannelController {

    private final ChannelService channelService;
    private final ChannelTestService channelTestService;

    public WorkspaceChannelController(ChannelService channelService,
                                      ChannelTestService channelTestService) {
        this.channelService = channelService;
        this.channelTestService = channelTestService;
    }

    @GetMapping
    public List<Channel> list(HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return channelService.listByUserId(principal.userId());
    }

    @GetMapping("/{id}")
    public Channel get(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return channelService.requireOwnedByUser(id, principal.userId());
    }

    @PostMapping
    public Channel create(@RequestBody ChannelRequest body, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        return channelService.createFromRequestForUser(principal.tenantId(), principal.userId(), body);
    }

    @PutMapping("/{id}")
    public Channel update(@PathVariable Long id,
                          @RequestBody ChannelRequest body,
                          HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        channelService.requireOwnedByUser(id, principal.userId());
        return channelService.updateFromRequest(id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        channelService.requireOwnedByUser(id, principal.userId());
        channelService.delete(id);
        return Map.of("message", "ok");
    }

    @PostMapping("/{id}/test")
    public Map<String, Object> test(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        Channel channel = channelService.requireOwnedByUser(id, principal.userId());
        return channelTestService.test(channel);
    }
}
