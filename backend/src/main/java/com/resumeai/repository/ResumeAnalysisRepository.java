package com.resumeai.repository;

import com.resumeai.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, UUID> {
    List<ResumeAnalysis> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<ResumeAnalysis> findFirstByDocumentIdOrderByCreatedAtDesc(UUID documentId);
    Optional<ResumeAnalysis> findByIdAndUserId(UUID id, UUID userId);
}

