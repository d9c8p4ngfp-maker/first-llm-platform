package com.first.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "conversation")
@Getter
@Setter
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Column(name = "key_entities", length = 4096)
    private String keyEntities;

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Column(name = "total_input_tokens", nullable = false)
    private Long totalInputTokens = 0L;

    @Column(name = "total_output_tokens", nullable = false)
    private Long totalOutputTokens = 0L;

    @Column(name = "saved_tokens", nullable = false)
    private Long savedTokens = 0L;

    @Column(name = "window_size", nullable = false)
    private Integer windowSize = 10;

    @Column(length = 100)
    private String model;

    @Column(name = "context_config", length = 4096)
    private String contextConfig;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
