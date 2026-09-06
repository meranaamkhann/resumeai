package com.resumeai.repository;

import com.resumeai.entity.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, UUID> {
    boolean existsByProviderAndEventId(String provider, String eventId);
}

