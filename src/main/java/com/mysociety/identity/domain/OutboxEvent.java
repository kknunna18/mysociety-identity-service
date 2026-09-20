package com.mysociety.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "outbox_events", schema = "mysociety")
public class OutboxEvent {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "society_id")
    private UUID societyId;
    @Column(name = "aggregate_type")
    private String aggregateType;
    @Column(name = "aggregate_id")
    private UUID aggregateId;
    @Column(name = "event_type")
    private String eventType;
    @Column(name = "event_version")
    private int eventVersion;
    @Column(columnDefinition = "jsonb")
    private String payload;
    @Column(columnDefinition = "jsonb")
    private String headers;
    private String status;
    @Column(name = "attempt_count")
    private int attemptCount;
    @Column(name = "available_at")
    private Instant availableAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "last_error")
    private String lastError;
    @Column(name = "created_at")
    private Instant createdAt;
}
