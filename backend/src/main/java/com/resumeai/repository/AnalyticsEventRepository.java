package com.resumeai.repository;

import com.resumeai.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {
    long countByEventType(String eventType);
}

