package com.first.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "missed_query")
@Getter
@Setter
public class MissedQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String query;

    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    @Column(name = "top_similarity", precision = 5, scale = 4)
    private BigDecimal topSimilarity;

    @Column(length = 100)
    private String scene;

    @Column(nullable = false)
    private Short resolved = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
