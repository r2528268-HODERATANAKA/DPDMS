package com.dpdms.auth_service.service;

import com.dpdms.auth_service.dto.CreateUserRequest;
import com.dpdms.auth_service.dto.UserResponse;
import com.dpdms.auth_service.exception.DuplicateResourceException;
import com.dpdms.auth_service.exception.ForbiddenOperationException;
import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests of the FR-SCOPE-01 rules enforced when a PROVINCIAL_ADMIN creates accounts. */
class UserAccountServiceTest {

    private final UserAccountRepository repository = mock(UserAccountRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserAccountService service = new UserAccountService(repository, encoder);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        Map<String, Object> details = new HashMap<>();
        details.put("ward", null);
        details.put("hazard", null);
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private CreateUserRequest request(Role role, String ward, String hazard) {
        return new CreateUserRequest("new.recorder", "Password123", "New Recorder",
                "new@dpdms.example", "+263771000099", role, ward, hazard);
    }

    @Test
    @DisplayName("Concrete ward+hazard recorder is accepted (201)")
    void concreteScopeAccepted() {
        loginAs("PROVINCIAL_ADMIN");
        when(repository.existsByUsernameIgnoreCase("new.recorder")).thenReturn(false);
        when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.create(request(Role.WARD_RECORDER, "Ward 3", "flood"));

        assertEquals("WARD_RECORDER", response.role());
        assertEquals("Ward 3", response.ward());
        assertEquals("flood", response.hazard());
    }

    @Test
    @DisplayName("Wildcard ward on a recorder is rejected (400) - FR-SCOPE-01")
    void wildcardWardRejected() {
        loginAs("PROVINCIAL_ADMIN");
        when(repository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(Role.WARD_RECORDER, "*", "flood")));
    }

    @Test
    @DisplayName("Blank hazard on a recorder is rejected (400) - FR-SCOPE-01")
    void blankHazardRejected() {
        loginAs("PROVINCIAL_ADMIN");
        when(repository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(Role.WARD_RECORDER, "Ward 3", " ")));
    }

    @Test
    @DisplayName("Supervisor without a hazard is rejected (400)")
    void supervisorWithoutHazardRejected() {
        loginAs("PROVINCIAL_ADMIN");
        when(repository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(Role.PROVINCIAL_SUPERVISOR, null, null)));
    }

    @Test
    @DisplayName("Only PROVINCIAL_ADMIN may create users (403 otherwise)")
    void onlyAdminCreatesUsers() {
        loginAs("WARD_RECORDER");
        assertThrows(ForbiddenOperationException.class,
                () -> service.create(request(Role.WARD_RECORDER, "Ward 3", "flood")));
    }

    @Test
    @DisplayName("Duplicate username is 409")
    void duplicateUsernameConflicts() {
        loginAs("PROVINCIAL_ADMIN");
        when(repository.existsByUsernameIgnoreCase("new.recorder")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> service.create(request(Role.WARD_RECORDER, "Ward 3", "flood")));
    }

    @Test
    @DisplayName("Password is stored BCrypt-hashed, never plaintext")
    void passwordStoredHashed() {
        loginAs("PROVINCIAL_ADMIN");
        when(repository.existsByUsernameIgnoreCase("new.recorder")).thenReturn(false);
        when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request(Role.WARD_RECORDER, "Ward 3", "flood"));

        org.mockito.ArgumentCaptor<UserAccount> captor =
                org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        String storedHash = captor.getValue().getPasswordHash();
        assertNotEquals("Password123", storedHash);
        assertTrue(encoder.matches("Password123", storedHash));
    }
}
