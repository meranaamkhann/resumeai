package com.resumeai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_webhook_events", uniqueConstraints = {
        @UniqueConstraint(name = "uq_webhook_provider_event", columnNames = {"provider", "event_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedWebhookEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        processedAt = Instant.now();
    }
}

