package com.mysociety.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "permissions", schema = "mysociety")
public class Permission {
    @Id
    @GeneratedValue
    private UUID id;
    private String code;
    @Column(name = "module_name")
    private String moduleName;
    private String description;
    @Column(name = "created_at")
    private Instant createdAt;
}
