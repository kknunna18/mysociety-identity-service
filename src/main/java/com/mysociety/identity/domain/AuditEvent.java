package com.mysociety.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_events", schema = "mysociety")
public class AuditEvent {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "society_id")
    private UUID societyId;
    @Column(name = "actor_user_id")
    private UUID actorUserId;
    @Column(name = "actor_type")
    private String actorType;
    private String action;
    @Column(name = "module_name")
    private String moduleName;
    @Column(name = "entity_type")
    private String entityType;
    @Column(name = "entity_id")
    private UUID entityId;
    private String outcome;
    @Column(name = "occurred_at")
    private Instant occurredAt;
}
