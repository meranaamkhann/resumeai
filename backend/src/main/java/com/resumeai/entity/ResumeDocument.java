package com.resumeai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_documents", indexes = {
        @Index(name = "idx_documents_resume", columnList = "resume_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "detected_mime_type")
    private String detectedMimeType;

    @Column(name = "declared_extension")
    private String declaredExtension;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "parsing_confidence")
    private Integer parsingConfidence;

    @Column(name = "parsing_issues", columnDefinition = "TEXT")
    private String parsingIssuesJson;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.UPLOADED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public enum Status {
        UPLOADED, PARSING, PARSED, PARSE_FAILED, ANALYZING, ANALYZED, ANALYSIS_FAILED
    }
}

