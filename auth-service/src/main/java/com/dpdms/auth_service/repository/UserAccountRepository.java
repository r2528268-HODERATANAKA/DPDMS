package com.dpdms.auth_service.repository;

import com.dpdms.auth_service.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    // Login and token validation look users up by username
    Optional<UserAccount> findByUsername(String username);

    // Fast duplicate check when the admin creates a new account (case-insensitive)
    boolean existsByUsernameIgnoreCase(String username);
}
