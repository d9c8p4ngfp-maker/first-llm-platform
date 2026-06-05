package com.first.gateway.domain.entity;

import com.first.gateway.domain.enums.ModelTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "routing_prototype")
@Getter
@Setter
public class RoutingPrototype {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_tier", nullable = false, length = 20)
    private ModelTier targetTier = ModelTier.STANDARD;

    @Column(name = "prototype_text", nullable = false, length = 500)
    private String prototypeText;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedding_vector", columnDefinition = "BLOB")
    private byte[] embeddingVector;

    @Column(nullable = false)
    private Short enabled = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
