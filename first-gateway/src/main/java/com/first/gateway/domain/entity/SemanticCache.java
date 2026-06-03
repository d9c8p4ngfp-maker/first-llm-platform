package com.first.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "semantic_cache")
@Getter
@Setter
public class SemanticCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "prompt_hash", nullable = false, length = 64)
    private String promptHash;

    @Column(name = "prompt_text", columnDefinition = "LONGTEXT")
    private String promptText;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String response;

    @Column(name = "prompt_tokens", nullable = false)
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    private Integer completionTokens = 0;

    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_hit_at")
    private Instant lastHitAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
