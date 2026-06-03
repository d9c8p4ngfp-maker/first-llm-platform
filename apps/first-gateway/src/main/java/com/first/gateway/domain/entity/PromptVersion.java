package com.first.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "prompt_version")
@Getter
@Setter
public class PromptVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(name = "system_prompt", columnDefinition = "LONGTEXT")
    private String systemPrompt;

    @Column(name = "user_prompt_template", columnDefinition = "LONGTEXT")
    private String userPromptTemplate;

    @Column(length = 4096)
    private String variables;

    @Column(name = "suggested_model", length = 100)
    private String suggestedModel;

    @Column(name = "ab_weight", nullable = false)
    private Integer abWeight = 100;

    @Column(length = 1000)
    private String changelog;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
