package com.resumeai.repository;

import com.resumeai.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {
    Optional<JobMatch> findFirstByDocumentIdAndJobDescriptionIdOrderByCreatedAtDesc(UUID documentId, UUID jobDescriptionId);
    Optional<JobMatch> findByIdAndUserId(UUID id, UUID userId);
}

