package com.first.gateway.domain.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter; import java.time.Instant;
@Entity @Table(name = "skill_binding") @Getter @Setter
public class SkillBinding {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "skill_id", nullable = false) private Long skillId;
    @Column(name = "binding_type", nullable = false, length = 20) private String bindingType;
    @Column(name = "binding_id", nullable = false) private Long bindingId;
    @Column(length = 4096) private String config;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
}