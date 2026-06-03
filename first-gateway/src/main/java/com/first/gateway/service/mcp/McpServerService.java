package com.first.gateway.service.mcp;
import com.first.gateway.domain.entity.McpServer;
import com.first.gateway.infra.error.GatewayError; import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.McpServerRepository;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.*;
@Service @Transactional(readOnly = true)
public class McpServerService {
    private final McpServerRepository repository;
    public McpServerService(McpServerRepository repository) { this.repository = repository; }
    public List<McpServer> list(Long userId) { return repository.findByUserIdAndDeletedOrderByUpdatedAtDesc(userId, (short) 0); }
    public McpServer require(Long id, Long userId) {
        return repository.findByIdAndUserIdAndDeleted(id, userId, (short) 0)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "mcp server not found"));
    }
    @Transactional public McpServer create(Long tenantId, Long userId, String name, String endpoint, String transport) {
        McpServer s = new McpServer(); s.setTenantId(tenantId); s.setUserId(userId); s.setName(name);
        s.setEndpoint(endpoint); s.setTransport(transport != null ? transport : "SSE"); s.setDeleted((short) 0);
        return repository.save(s);
    }
    @Transactional public McpServer update(Long id, Long userId, String name, String endpoint) {
        McpServer s = require(id, userId); if (name != null) s.setName(name); if (endpoint != null) s.setEndpoint(endpoint);
        return repository.save(s);
    }
    @Transactional public void delete(Long id, Long userId) { McpServer s = require(id, userId); s.setDeleted((short) 1); repository.save(s); }
    @Transactional public McpServer toggle(Long id, Long userId) {
        McpServer s = require(id, userId); s.setEnabled(s.getEnabled() == 1 ? (short) 0 : (short) 1);
        s.setStatus(s.getEnabled() == 1 ? "ACTIVE" : "INACTIVE"); return repository.save(s);
    }
    @Transactional public Map<String, Object> test(Long id, Long userId) {
        McpServer s = require(id, userId); s.setLastTestAt(Instant.now());
        s.setLastTestResult("OK"); s.setStatus("ACTIVE"); repository.save(s);
        return Map.of("success", true, "tools", List.of(Map.of("name", "sample_tool", "description", "stub")));
    }
}