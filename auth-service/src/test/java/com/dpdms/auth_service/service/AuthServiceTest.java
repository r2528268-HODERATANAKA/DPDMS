package com.dpdms.auth_service.service;

import com.dpdms.auth_service.dto.LoginResponse;
import com.dpdms.auth_service.dto.ValidationResponse;
import com.dpdms.auth_service.exception.InvalidCredentialsException;
import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.repository.UserAccountRepository;
import com.dpdms.auth_service.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests of login + token validation - the origin of the team's token contract. */
class AuthServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-0123456789";

    private final UserAccountRepository repository = mock(UserAccountRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(SECRET, 60);
    private final AuthService authService = new AuthService(repository, jwtService, encoder);

    private UserAccount recorder;

    @BeforeEach
    void seedUser() {
        recorder = UserAccount.builder()
                .username("ward4.fire")
                .passwordHash(encoder.encode("Ward@123"))
                .fullName("Ward 4 Fire Recorder")
                .role(Role.WARD_RECORDER)
                .ward("Ward 4")
                .hazard("fire")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Login returns a token carrying the RBAC claims")
    void loginIssuesTokenWithScopeClaims() {
        when(repository.findByUsername("ward4.fire")).thenReturn(Optional.of(recorder));

        LoginResponse response = authService.login("ward4.fire", "Ward@123");

        assertEquals("ward4.fire", response.username());
        assertEquals("WARD_RECORDER", response.role());
        assertEquals("Ward 4", response.ward());
        assertEquals("fire", response.hazard());

        Claims claims = jwtService.parse(response.token());
        assertEquals("ward4.fire", claims.getSubject());
        assertEquals("WARD_RECORDER", claims.get("role", String.class));
        assertEquals("Ward 4", claims.get("ward", String.class));
        assertEquals("fire", claims.get("hazard", String.class));
    }

    @Test
    @DisplayName("Wrong password is 401")
    void wrongPasswordRejected() {
        when(repository.findByUsername("ward4.fire")).thenReturn(Optional.of(recorder));
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login("ward4.fire", "WrongPass1"));
    }

    @Test
    @DisplayName("Unknown username is 401")
    void unknownUserRejected() {
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> authService.login("ghost", "whatever"));
    }

    @Test
    @DisplayName("Disabled account is 401")
    void disabledAccountRejected() {
        recorder.setActive(false);
        when(repository.findByUsername("ward4.fire")).thenReturn(Optional.of(recorder));
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login("ward4.fire", "Ward@123"));
    }

    @Test
    @DisplayName("validate() decodes a good token to the caller's scope")
    void validateDecodesScope() {
        ValidationResponse response = authService.validate(jwtService.issue(recorder));

        assertTrue(response.valid());
        assertEquals("ward4.fire", response.username());
        assertEquals("WARD_RECORDER", response.role());
        assertEquals("Ward 4", response.ward());
        assertEquals("fire", response.hazard());
    }
}
