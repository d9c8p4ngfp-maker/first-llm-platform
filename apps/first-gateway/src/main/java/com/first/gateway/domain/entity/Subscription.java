package com.first.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "subscription")
@Getter
@Setter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 20)
    private String plan = "FREE";

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "monthly_quota", nullable = false)
    private Long monthlyQuota = 0L;

    @Column(name = "daily_rate_limit", nullable = false)
    private Integer dailyRateLimit = 100;

    @Column(length = 4096)
    private String features;

    @Column(name = "quota_reset_period", nullable = false, length = 20)
    private String quotaResetPeriod = "MONTHLY";

    @Column(name = "last_reset_at")
    private Instant lastResetAt;

    @Column(name = "next_reset_at")
    private Instant nextResetAt;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (startedAt == null) {
            startedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
