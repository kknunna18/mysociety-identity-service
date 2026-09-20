package com.mysociety.identity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
class RolePermissionId implements Serializable {
    private UUID roleId;
    private UUID permissionId;
}

@Getter
@Setter
@Entity
@Table(name = "role_permissions", schema = "mysociety")
public class RolePermission {
    @EmbeddedId
    private RolePermissionId id;
    @Column(name = "created_at")
    private Instant createdAt;
}
