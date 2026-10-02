package com.mysociety.identity.service;

import com.mysociety.identity.api.AuthDtos.*;
import com.mysociety.identity.api.UserMapper;
import com.mysociety.identity.config.JwtProperties;
import com.mysociety.identity.domain.*;
import com.mysociety.identity.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class IdentityService {
    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final UserSocietyRoleRepository assignments;
    private final RoleRepository roles;
    private final RolePermissionRepository rolePermissions;
    private final AuditEventRepository audits;
    private final UserMapper mapper;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final JwtProperties jwt;

    public IdentityService(AppUserRepository users, RefreshTokenRepository refreshTokens, UserSocietyRoleRepository assignments, RoleRepository roles, RolePermissionRepository rolePermissions, AuditEventRepository audits, UserMapper mapper, PasswordEncoder passwords, JwtEncoder encoder, JwtProperties jwt) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.assignments = assignments;
        this.roles = roles;
        this.rolePermissions = rolePermissions;
        this.audits = audits;
        this.mapper = mapper;
        this.passwords = passwords;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String ip, String agent) {
        AppUser user = request.identifier().contains("@") ? users.findByEmailIgnoreCase(request.identifier()).orElseThrow(this::unauthorized) : users.findByMobileNumber(request.identifier()).orElseThrow(this::unauthorized);
        if (user.getStatus() != UserStatus.ACTIVE || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) || !passwords.matches(request.password(), user.getPasswordHash())) {
            audit(user.getId(), "LOGIN", "FAILURE");
            throw unauthorized();
        }
        user.setFailedLoginCount(0);
        user.setLastLoginAt(Instant.now());
        audit(user.getId(), "LOGIN", "SUCCESS");
        return issue(user, ip, agent);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request, String ip, String agent) {
        RefreshToken old = refreshTokens.findByTokenHash(hash(request.refreshToken())).orElseThrow(this::unauthorized);
        if (old.getRevokedAt() != null || old.getExpiresAt().isBefore(Instant.now())) throw unauthorized();
        AppUser user = users.findById(old.getUserId()).orElseThrow(this::unauthorized);
        if (user.getStatus() != UserStatus.ACTIVE) throw unauthorized();
        old.setRevokedAt(Instant.now());
        TokenResponse response = issue(user, ip, agent);
        old.setReplacedByTokenId(refreshTokens.findByTokenHash(hash(response.refreshToken())).orElseThrow().getId());
        audit(user.getId(), "TOKEN_REFRESH", "SUCCESS");
        return response;
    }

    @Transactional
    public void logout(UUID actor, String raw) {
        refreshTokens.findByTokenHash(hash(raw)).filter(t -> t.getUserId().equals(actor)).ifPresent(t -> {
            t.setRevokedAt(Instant.now());
            audit(actor, "LOGOUT", "SUCCESS");
        });
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID id) {
        return user(id);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public UserResponse user(UUID id) {
        return response(users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }

    @Transactional
    public InvitationResponse invite(InvitationRequest r, UUID actor) {
        if (r.email() == null && r.mobileNumber() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or mobile number is required");
        AppUser u = new AppUser();
        u.setEmail(r.email());
        u.setMobileNumber(r.mobileNumber());
        u.setFirstName(r.firstName());
        u.setLastName(r.lastName());
        u.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
        u = users.save(u);
        UserSocietyRole a = new UserSocietyRole();
        a.setUserId(u.getId());
        a.setSocietyId(r.societyId());
        a.setRoleId(r.roleId());
        a.setGrantedBy(actor);
        assignments.save(a);
        audit(actor, "INVITE_USER", "SUCCESS");
        return new InvitationResponse(u.getId(), u.getStatus().name(), null);
    }

    @Transactional
    public UserResponse status(UUID id, UserStatus status, UUID actor) {
        AppUser u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        u.setStatus(status);
        audit(actor, "UPDATE_USER_STATUS", "SUCCESS");
        return response(u);
    }

    @Transactional
    public void assign(UUID id, RoleAssignmentRequest r, UUID actor) {
        if (!users.existsById(id) || !roles.existsById(r.roleId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User or role not found");
        UserSocietyRole a = new UserSocietyRole();
        a.setUserId(id);
        a.setSocietyId(r.societyId());
        a.setRoleId(r.roleId());
        a.setValidUntil(r.validUntil());
        a.setGrantedBy(actor);
        assignments.save(a);
        audit(actor, "ASSIGN_ROLE", "SUCCESS");
    }

    @Transactional
    public void revoke(UUID id, UUID roleId, UUID societyId, UUID actor) {
        UserSocietyRole a = assignments.findByUserIdAndRoleIdAndSocietyIdAndActiveTrue(id, roleId, societyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active role assignment not found"));
        a.setActive(false);
        a.setValidUntil(LocalDate.now());
        audit(actor, "REVOKE_ROLE", "SUCCESS");
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles(UUID societyId) {
        return roles.findByActiveTrueAndSocietyIdIsNullOrActiveTrueAndSocietyId(societyId).stream().map(mapper::toResponse).toList();
    }

    private TokenResponse issue(AppUser user, String ip, String agent) {
        Instant now = Instant.now();
        List<String> scopes = scopes(user.getId());
        String access = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), JwtClaimsSet.builder().issuer("mysociety-identity").subject(user.getId().toString()).issuedAt(now).expiresAt(now.plus(jwt.accessTtl())).claim("scope", String.join(" ", scopes)).build())).getTokenValue();
        String raw = random();
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(raw));
        token.setIpAddress(ip);
        token.setUserAgent(agent);
        token.setIssuedAt(now);
        token.setExpiresAt(now.plus(jwt.refreshTtl()));
        refreshTokens.save(token);
        return new TokenResponse(access, raw, "Bearer", now.plus(jwt.accessTtl()));
    }

    private UserResponse response(AppUser u) {
        UserResponse r = mapper.toResponse(u);
        List<String> roleCodes = roleCodes(u.getId());
        return new UserResponse(r.id(), r.email(), r.mobileNumber(), r.firstName(), r.lastName(), r.status(), roleCodes, scopes(u.getId()));
    }

    private List<String> roleCodes(UUID user) {
        return assignments.findAll().stream().filter(a -> a.getUserId().equals(user) && a.isActive() && (a.getValidUntil() == null || !a.getValidUntil().isBefore(LocalDate.now()))).map(UserSocietyRole::getRoleId).distinct().map(id -> roles.findById(id).map(Role::getCode).orElse(null)).filter(Objects::nonNull).toList();
    }

    private List<String> scopes(UUID user) {
        List<UUID> ids = assignments.findAll().stream().filter(a -> a.getUserId().equals(user) && a.isActive() && (a.getValidUntil() == null || !a.getValidUntil().isBefore(LocalDate.now()))).map(UserSocietyRole::getRoleId).distinct().toList();
        return ids.isEmpty() ? List.of() : rolePermissions.permissionCodes(ids);
    }

    private void audit(UUID actor, String action, String outcome) {
        AuditEvent e = new AuditEvent();
        e.setActorUserId(actor);
        e.setActorType("USER");
        e.setAction(action);
        e.setModuleName("IDENTITY");
        e.setOutcome(outcome);
        e.setOccurredAt(Instant.now());
        audits.save(e);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials or token");
    }

    private String random() {
        byte[] b = new byte[48];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String hash(String v) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
