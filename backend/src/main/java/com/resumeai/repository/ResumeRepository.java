package com.resumeai.repository;

import com.resumeai.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByUserIdAndDeletedAtIsNull(UUID userId);
}

