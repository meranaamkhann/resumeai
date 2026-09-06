package com.resumeai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_analyses", indexes = {
        @Index(name = "idx_analyses_document", columnList = "document_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysis {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "ats_parsing_score", nullable = false)
    private int atsParsingScore;

    @Column(name = "keyword_alignment_score", nullable = false)
    private int keywordAlignmentScore;

    @Column(name = "structure_score", nullable = false)
    private int structureScore;

    @Column(name = "content_quality_score", nullable = false)
    private int contentQualityScore;

    @Column(name = "experience_relevance_score", nullable = false)
    private int experienceRelevanceScore;

    @Column(name = "impact_score", nullable = false)
    private int impactScore;

    @Column(name = "formatting_score", nullable = false)
    private int formattingScore;

    @Column(name = "issues", columnDefinition = "TEXT", nullable = false)
    private String issuesJson;

    @Column(name = "detected_sections", columnDefinition = "TEXT")
    private String detectedSectionsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

