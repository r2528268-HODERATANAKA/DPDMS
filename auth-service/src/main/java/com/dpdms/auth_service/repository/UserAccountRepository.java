package com.dpdms.auth_service.repository;

import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<UserAccount> findByRole(Role role);

    /** Recipients for alert fan-out: recorders of a (ward, hazard) pair. */
    List<UserAccount> findByRoleAndWardAndHazard(Role role, String ward, String hazard);

    /** Supervisors of one hazard. */
    List<UserAccount> findByRoleAndHazard(Role role, String hazard);
}
