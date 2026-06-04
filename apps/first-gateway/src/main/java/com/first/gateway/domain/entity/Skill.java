package com.first.gateway.domain.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter; import java.time.Instant;
@Entity @Table(name = "skill") @Getter @Setter
public class Skill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 2000) private String description;
    @Column(length = 50) private String icon;
    @Column(name = "prompt_template_id") private Long promptTemplateId;
    @Column(name = "suggested_model", length = 100) private String suggestedModel;
    @Column(name = "model_params", columnDefinition = "JSON") private String modelParams;
    @Column(nullable = false) private Short enabled = 1;
    @Column(nullable = false, length = 20) private String visibility = "PRIVATE";
    @Column(name = "usage_count", nullable = false) private Integer usageCount = 0;
    @Column(name = "sort_order", nullable = false) private Integer sortOrder = 0;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(nullable = false) private Short deleted = 0;
    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}