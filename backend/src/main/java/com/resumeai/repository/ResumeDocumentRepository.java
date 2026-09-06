package com.resumeai.repository;

import com.resumeai.entity.ResumeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeDocumentRepository extends JpaRepository<ResumeDocument, UUID> {
    Optional<ResumeDocument> findByIdAndUserId(UUID id, UUID userId);
    List<ResumeDocument> findByResumeIdAndUserId(UUID resumeId, UUID userId);
    Optional<ResumeDocument> findByUserIdAndContentHash(UUID userId, String contentHash);
}

