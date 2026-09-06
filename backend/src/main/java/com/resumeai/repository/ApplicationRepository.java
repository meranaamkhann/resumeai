package com.resumeai.repository;

import com.resumeai.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);
}

