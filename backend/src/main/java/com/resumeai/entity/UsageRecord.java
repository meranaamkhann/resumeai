package com.resumeai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_records", uniqueConstraints = {
        @UniqueConstraint(name = "uq_usage_user_period", columnNames = {"user_id", "period"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "analysis_count", nullable = false)
    @Builder.Default
    private int analysisCount = 0;

    @Column(name = "jd_match_count", nullable = false)
    @Builder.Default
    private int jdMatchCount = 0;

    @Column(name = "optimization_count", nullable = false)
    @Builder.Default
    private int optimizationCount = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}

