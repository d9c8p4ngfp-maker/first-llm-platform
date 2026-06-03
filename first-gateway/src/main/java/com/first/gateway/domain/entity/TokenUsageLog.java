package com.first.gateway.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "token_usage_log")
@Getter
@Setter
public class TokenUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(length = 100)
    private String model;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "cost_ratio", precision = 6, scale = 3)
    private BigDecimal costRatio;

    @Column(name = "unit_type", nullable = false, length = 20)
    private String unitType = "TOKEN";

    @Column(name = "cache_hit", nullable = false)
    private Short cacheHit = 0;

    @Column(name = "rag_used", nullable = false)
    private Short ragUsed = 0;

    @Column(name = "routing_model", length = 100)
    private String routingModel;

    @Column(name = "is_stream", nullable = false)
    private Short isStream = 0;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(length = 20)
    private String status;

    @Column(name = "error_msg", length = 500)
    private String errorMsg;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
