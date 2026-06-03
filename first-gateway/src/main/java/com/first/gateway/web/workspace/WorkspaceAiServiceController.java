package com.first.gateway.web.workspace;

import com.first.gateway.infra.ai.AiServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class WorkspaceAiServiceController {

    private final AiServiceClient aiServiceClient;

    public WorkspaceAiServiceController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @GetMapping("/ai-health")
    public ResponseEntity<Map<String, Object>> aiHealth() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", aiServiceClient.isEnabled());
        body.put("base_url", aiServiceClient.baseUrl());
        if (!aiServiceClient.isEnabled()) {
            body.put("status", "disabled");
            return ResponseEntity.ok(body);
        }
        var health = aiServiceClient.fetchHealth();
        if (health.isPresent()) {
            body.put("status", "ok");
            body.put("ai_service", health.get());
            return ResponseEntity.ok(body);
        }
        body.put("status", "unreachable");
        return ResponseEntity.status(503).body(body);
    }
}