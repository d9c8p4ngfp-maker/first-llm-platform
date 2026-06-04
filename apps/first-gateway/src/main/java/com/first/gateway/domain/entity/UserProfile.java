package com.first.gateway.domain.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter; import java.time.Instant;
@Entity @Table(name = "user_profile") @Getter @Setter
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(length = 100) private String nickname;
    @Column(length = 10) private String mbti;
    @Column(name = "mbti_label", length = 50) private String mbtiLabel;
    @Column(length = 20) private String zodiac;
    @Column(name = "primary_tag", length = 100) private String primaryTag;
    @Column(name = "ai_summary", length = 4000) private String aiSummary;
    @Column(name = "ai_tags", length = 4096) private String aiTags;
    @Column(name = "memory_count", nullable = false) private Integer memoryCount = 0;
    @Column(name = "ai_system_prompt", columnDefinition = "TEXT") private String aiSystemPrompt;
    @Column(name = "ai_personality", length = 4096) private String aiPersonality;
    @Column(name = "last_analyzed_at") private Instant lastAnalyzedAt;
    @Column(name = "profile_enabled", nullable = false) private Short profileEnabled = 1;
    @Version
    @Column(nullable = false) private Integer version = 0;
    @Column(name = "last_synthesis_count") private Integer lastSynthesisCount = 0;
    @Column(name = "synthesis_status", nullable = false, length = 20) private String synthesisStatus = "IDLE";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}