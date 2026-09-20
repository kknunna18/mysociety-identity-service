package com.mysociety.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "roles", schema = "mysociety")
public class Role {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "society_id")
    private UUID societyId;
    private String code;
    private String name;
    private String description;
    @Column(name = "is_system")
    private boolean system;
    @Column(name = "is_active")
    private boolean active;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Version
    private long version;
}
