package com.first.gateway.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.first.gateway.domain.enums.ChannelStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "channel")
@Getter
@Setter
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 50)
    private String provider;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @JsonIgnore
    @Column(name = "api_key_encrypted", nullable = false, length = 500)
    private String apiKeyEncrypted;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(nullable = false)
    private Integer weight = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelStatus status = ChannelStatus.ACTIVE;

    @Column(name = "max_rpm", nullable = false)
    private Integer maxRpm = 0;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount = 0;

    @Column(name = "used_quota", nullable = false)
    private Long usedQuota = 0L;

    @Column(length = 4096)
    private String config;

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
