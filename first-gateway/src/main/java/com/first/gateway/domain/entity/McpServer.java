package com.first.gateway.domain.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter; import java.time.Instant;
@Entity @Table(name = "mcp_server") @Getter @Setter
public class McpServer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "server_type", length = 50) private String serverType;
    @Column(nullable = false, length = 20) private String transport = "SSE";
    @Column(length = 500) private String endpoint;
    @Column(length = 500) private String command;
    @Column(name = "env_config", length = 4096) private String envConfig;
    @Column(length = 4096) private String tools;
    @Column(nullable = false, length = 20) private String status = "INACTIVE";
    @Column(nullable = false) private Short enabled = 1;
    @Column(name = "last_test_at") private Instant lastTestAt;
    @Column(name = "last_test_result", length = 2000) private String lastTestResult;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(nullable = false) private Short deleted = 0;
    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}