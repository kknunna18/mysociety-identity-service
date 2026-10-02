package com.mysociety.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens", schema = "mysociety")
public class RefreshToken {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "token_hash")
    private String tokenHash;
    @Column(name = "device_name")
    private String deviceName;
    @Column(name = "ip_address", columnDefinition = "inet")
    @JdbcTypeCode(SqlTypes.INET)
    private String ipAddress;
    @Column(name = "user_agent")
    private String userAgent;
    @Column(name = "issued_at")
    private Instant issuedAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;
}
