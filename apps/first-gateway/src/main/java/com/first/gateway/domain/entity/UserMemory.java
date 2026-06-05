package com.first.gateway.domain.entity;
import com.first.gateway.domain.enums.MemoryCategory;
import com.first.gateway.domain.enums.MemoryStatus;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter; import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Entity @Table(name = "user_memory") @Getter @Setter
public class UserMemory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "conversation_id") private Long conversationId;
    @Column(name = "message_id") private Long messageId;
    @Column(nullable = false, length = 20) private String source = "MANUAL";
    @Column(nullable = false, length = 30) @Enumerated(EnumType.STRING) private MemoryCategory category;
    @Column(nullable = false, length = 4000) private String content;
    @Column(nullable = false) private Short importance = 3;
    @Column(name = "schedule_date") private LocalDate scheduleDate;
    @Column(name = "schedule_time", length = 10) private String scheduleTime;
    @Column(name = "valid_until") private Instant validUntil;
    @Column(name = "numeric_value") private BigDecimal numericValue;
    @Column(nullable = false, length = 20) @Enumerated(EnumType.STRING) private MemoryStatus status = MemoryStatus.ACTIVE;
    @Column private Short reminded = 0;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}