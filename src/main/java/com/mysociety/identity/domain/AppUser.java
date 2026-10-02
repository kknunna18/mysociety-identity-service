package com.mysociety.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "app_users", schema = "mysociety")
public class AppUser {
    @Id
    @GeneratedValue
    private UUID id;
    private String email;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    @Column(name = "preferred_language")
    private String preferredLanguage = "en";
    private String timezone = "Asia/Kolkata";
    @Column(name = "email_verified")
    private boolean emailVerified;
    @Column(name = "mobile_verified")
    private boolean mobileVerified;
    @Column(name = "mfa_enabled")
    private boolean mfaEnabled;
    @Column(name = "failed_login_count")
    private int failedLoginCount;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Version
    private long version;

    @PrePersist
    void created() {
        createdAt = updatedAt = Instant.now();
        if (status == null) status = UserStatus.INVITED;
    }

    @PreUpdate
    void updated() {
        updatedAt = Instant.now();
    }
}
