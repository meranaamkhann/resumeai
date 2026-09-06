package com.resumeai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_descriptions", indexes = {
        @Index(name = "idx_jd_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescription {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    private String rawText;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "seniority")
    private String seniority;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkillsJson;

    @Column(name = "preferred_skills", columnDefinition = "TEXT")
    private String preferredSkillsJson;

    @Column(name = "min_years_experience")
    private Integer minYearsExperience;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

