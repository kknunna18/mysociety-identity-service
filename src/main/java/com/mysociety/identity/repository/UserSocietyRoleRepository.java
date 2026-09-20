package com.mysociety.identity.repository;

import com.mysociety.identity.domain.UserSocietyRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSocietyRoleRepository extends JpaRepository<UserSocietyRole, UUID> {
    List<UserSocietyRole> findByUserIdAndActiveTrueAndValidFromLessThanEqualAndValidUntilIsNullOrUserIdAndActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(UUID u1, LocalDate d1, UUID u2, LocalDate d2, LocalDate d3);

    Optional<UserSocietyRole> findByUserIdAndRoleIdAndSocietyIdAndActiveTrue(UUID userId, UUID roleId, UUID societyId);
}
