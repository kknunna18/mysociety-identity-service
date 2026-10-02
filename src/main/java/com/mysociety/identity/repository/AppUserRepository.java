package com.mysociety.identity.repository;

import com.mysociety.identity.domain.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByMobileNumber(String mobileNumber);

    Page<AppUser> findAll(Pageable pageable);
}
