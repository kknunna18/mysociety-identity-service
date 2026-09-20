package com.mysociety.identity.repository;

import com.mysociety.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    List<Role> findByActiveTrueAndSocietyIdIsNullOrActiveTrueAndSocietyId(UUID societyId);
}
