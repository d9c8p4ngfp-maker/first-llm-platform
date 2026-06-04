package com.first.gateway.domain.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter; import java.time.Instant;
@Entity @Table(name = "pipeline_config") @Getter @Setter
public class PipelineConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "config_key", nullable = false, length = 100) private String configKey;
    @Column(nullable = false, length = 20) private String scope = "SYSTEM";
    @Column(name = "user_id", nullable = false)
    private Long userId = 0L;
    @Column(name = "model_id", length = 100) private String modelId;
    @Column(name = "model_params", columnDefinition = "JSON") private String modelParams;
    @Column(name = "prompt_template_id") private Long promptTemplateId;
    @Column(name = "prompt_text", columnDefinition = "TEXT") private String promptText;
    @Column(nullable = false) private Short enabled = 1;
    @Column(length = 500) private String description;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}