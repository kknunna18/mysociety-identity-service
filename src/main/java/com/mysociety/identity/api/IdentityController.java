package com.mysociety.identity.api;

import com.mysociety.identity.api.AuthDtos.*;
import com.mysociety.identity.service.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class IdentityController {
    private final IdentityService service;

    public IdentityController(IdentityService service) {
        this.service = service;
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Exchange credentials for access and rotating refresh tokens")
    public TokenResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest h) {
        return service.login(r, h.getRemoteAddr(), h.getHeader("User-Agent"));
    }

    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest r, HttpServletRequest h) {
        return service.refresh(r, h.getRemoteAddr(), h.getHeader("User-Agent"));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LogoutRequest r) {
        service.logout(UUID.fromString(jwt.getSubject()), r.refreshToken());
    }

    @GetMapping("/auth/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/users/invitations")
    @PreAuthorize("hasAuthority('SCOPE_users:manage')")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse invite(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody InvitationRequest r) {
        return service.invite(r, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('SCOPE_users:read')")
    public Page<UserResponse> users(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.list(p);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_users:read')")
    public UserResponse user(@PathVariable UUID userId) {
        return service.user(userId);
    }

    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasAuthority('SCOPE_users:manage')")
    public UserResponse status(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId, @Valid @RequestBody StatusRequest r) {
        return service.status(userId, r.status(), UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/roles")
    public List<RoleResponse> roles(@RequestParam(required = false) UUID societyId) {
        return service.roles(societyId);
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('SCOPE_users:manage')")
    @ResponseStatus(HttpStatus.CREATED)
    public void assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId, @Valid @RequestBody RoleAssignmentRequest r) {
        service.assign(userId, r, UUID.fromString(jwt.getSubject()));
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('SCOPE_users:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId, @PathVariable UUID roleId, @RequestParam UUID societyId) {
        service.revoke(userId, roleId, societyId, UUID.fromString(jwt.getSubject()));
    }
}
