package com.resumeai.repository;

import com.resumeai.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {
    Optional<JobDescription> findByIdAndUserId(UUID id, UUID userId);
}

