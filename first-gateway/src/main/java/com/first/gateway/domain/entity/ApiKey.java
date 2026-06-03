package com.first.gateway.domain.entity;

import com.first.gateway.domain.enums.ApiKeyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "api_key")
@Getter
@Setter
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 100)
    private String name;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    @Column(name = "remaining_quota", nullable = false)
    private Long remainingQuota = -1L;

    @Column(name = "used_quota", nullable = false)
    private Long usedQuota = 0L;

    @Column(name = "rate_limit", nullable = false)
    private Integer rateLimit = -1;

    @Column(name = "tpm_limit", nullable = false)
    private Integer tpmLimit = -1;

    @Column(name = "max_concurrent", nullable = false)
    private Integer maxConcurrent = -1;

    @Column(name = "rate_config", length = 4096)
    private String rateConfig;

    @Column(name = "allowed_models", length = 4096)
    private String allowedModels;

    @Column(name = "security_config", length = 4096)
    private String securityConfig;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Short deleted = 0;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
