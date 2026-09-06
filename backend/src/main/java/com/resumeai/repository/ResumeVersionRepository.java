package com.resumeai.repository;

import com.resumeai.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, UUID> {
    List<ResumeVersion> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<ResumeVersion> findByIdAndUserId(UUID id, UUID userId);
}

