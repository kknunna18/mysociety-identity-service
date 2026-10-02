package com.mysociety.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, Instant expiresAt) {
    }

    public record UserResponse(UUID id, String email, String mobileNumber, String firstName, String lastName,
                               String status, List<String> roles, List<String> scopes) {
    }

    public record InvitationRequest(@Email String email, @Pattern(regexp = "^[+0-9][0-9 -]{6,29}$") String mobileNumber,
                                    @NotBlank String firstName, String lastName, @NotNull UUID societyId,
                                    @NotNull UUID roleId) {
    }

    public record InvitationResponse(UUID userId, String status, String setupToken) {
    }

    public record StatusRequest(@NotNull com.mysociety.identity.domain.UserStatus status) {
    }

    public record RoleAssignmentRequest(@NotNull UUID societyId, @NotNull UUID roleId, LocalDate validUntil) {
    }

    public record RoleResponse(UUID id, String code, String name, String description, UUID societyId) {
    }
}
