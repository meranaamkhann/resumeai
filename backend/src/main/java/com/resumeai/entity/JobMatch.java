package com.resumeai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_matches", indexes = {
        @Index(name = "idx_job_matches_document", columnList = "document_id"),
        @Index(name = "idx_job_matches_jd", columnList = "job_description_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatch {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "job_description_id", nullable = false)
    private UUID jobDescriptionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "match_score", nullable = false)
    private int matchScore;

    @Column(name = "required_matched", nullable = false)
    private int requiredMatched;

    @Column(name = "required_total", nullable = false)
    private int requiredTotal;

    @Column(name = "preferred_matched", nullable = false)
    private int preferredMatched;

    @Column(name = "preferred_total", nullable = false)
    private int preferredTotal;

    @Column(name = "experience_match_label")
    private String experienceMatchLabel;

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkillsJson;

    @Column(name = "missing_required_skills", columnDefinition = "TEXT")
    private String missingRequiredSkillsJson;

    @Column(name = "missing_preferred_skills", columnDefinition = "TEXT")
    private String missingPreferredSkillsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

