package com.first.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "console_user_preference")
@Getter
@Setter
public class ConsoleUserPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "default_model", length = 100)
    private String defaultModel;

    @Column(name = "preferences_json", length = 4096)
    private String preferencesJson;

    @Column(length = 20)
    private String language = "zh-CN";

    @Column(name = "routing_priority", length = 500)
    private String routingPriority;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}