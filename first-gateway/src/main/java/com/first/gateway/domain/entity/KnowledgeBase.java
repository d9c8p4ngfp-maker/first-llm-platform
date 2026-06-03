package com.first.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "knowledge_base")
@Getter
@Setter
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String industry;

    @Column(name = "rag_platform", nullable = false, length = 50)
    private String ragPlatform = "BAILIAN";

    @Column(name = "external_index_id", length = 200)
    private String externalIndexId;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedding_provider", length = 50)
    private String embeddingProvider;

    @Column(name = "retrieval_config", length = 4096)
    private String retrievalConfig;

    @Column(name = "process_config", length = 4096)
    private String processConfig;

    @Column(name = "doc_count", nullable = false)
    private Integer docCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false, length = 20)
    private String visibility = "PRIVATE";

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
