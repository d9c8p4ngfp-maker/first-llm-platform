package com.first.gateway.domain.entity;

import com.first.gateway.domain.enums.SourceType;
import com.first.gateway.domain.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "knowledge_document")
@Getter
@Setter
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_type", length = 20)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType = SourceType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(name = "external_doc_id", length = 200)
    private String externalDocId;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount = 0;

    @Column(name = "quality_score", precision = 3, scale = 1)
    private BigDecimal qualityScore;

    @Column(name = "doc_version", length = 50)
    private String docVersion;

    @Column(name = "authority_level", nullable = false, length = 20)
    private String authorityLevel = "OFFICIAL";

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "index_error", length = 500)
    private String indexError;

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
