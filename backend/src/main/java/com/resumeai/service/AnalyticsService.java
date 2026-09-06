package com.resumeai.service;

import com.resumeai.entity.AnalyticsEvent;
import com.resumeai.repository.AnalyticsEventRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;

    public AnalyticsService(AnalyticsEventRepository analyticsEventRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
    }

    @Async
    public void record(UUID userId, String eventType) {
        analyticsEventRepository.save(AnalyticsEvent.builder()
                .userId(userId)
                .eventType(eventType)
                .build());
    }
}

