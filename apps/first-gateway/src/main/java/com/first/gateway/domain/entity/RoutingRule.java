package com.first.gateway.domain.entity;

import com.first.gateway.domain.enums.ModelTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "routing_rule")
@Getter
@Setter
public class RoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    @Column(name = "condition_config", nullable = false, length = 4096)
    private String conditionConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_tier", nullable = false, length = 20)
    private ModelTier targetTier = ModelTier.STANDARD;

    @Column(name = "target_model", length = 100)
    private String targetModel;

    @Column(nullable = false)
    private Short enabled = 1;

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
