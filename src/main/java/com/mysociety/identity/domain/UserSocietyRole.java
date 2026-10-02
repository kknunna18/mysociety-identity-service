package com.mysociety.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_society_roles", schema = "mysociety")
public class UserSocietyRole {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "society_id")
    private UUID societyId;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "role_id")
    private UUID roleId;
    @Column(name = "valid_from")
    private LocalDate validFrom;
    @Column(name = "valid_until")
    private LocalDate validUntil;
    @Column(name = "is_active")
    private boolean active;
    @Column(name = "granted_by")
    private UUID grantedBy;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Version
    private long version;

    @PrePersist
    void created() {
        if (validFrom == null) validFrom = LocalDate.now();
        active = true;
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void updated() {
        updatedAt = Instant.now();
    }
}
