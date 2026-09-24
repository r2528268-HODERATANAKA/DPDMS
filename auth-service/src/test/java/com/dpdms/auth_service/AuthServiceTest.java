package com.dpdms.auth_service;

import com.dpdms.auth_service.dto.CreateUserRequest;
import com.dpdms.auth_service.dto.LoginRequest;
import com.dpdms.auth_service.dto.LoginResponse;
import com.dpdms.auth_service.dto.ValidationResponse;
import com.dpdms.auth_service.exception.DuplicateResourceException;
import com.dpdms.auth_service.exception.ForbiddenOperationException;
import com.dpdms.auth_service.exception.InvalidCredentialsException;
import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.repository.UserAccountRepository;
import com.dpdms.auth_service.service.AuthService;
import com.dpdms.auth_service.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for the auth business rules (repository mocked out).
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET =
            "dpdms-test-secret-key-which-is-long-enough-0123456789abcdef";

    @Mock
    private UserAccountRepository repository;

    @Mock
    private JwtTokenService jwtTokenService; // real crypto covered by JwtTokenServiceTest

    @InjectMocks
    private AuthService service;

    private UserAccount recorder;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        recorder = UserAccount.builder()
                .id(1L).username("tanaka")
                .passwordHash(encoder.encode("Passw0rd!"))
                .fullName("Tanaka M.").role(Role.WARD_RECORDER)
                .ward("Mudzi").hazard("flood").active(true)
                .build();
    }

    // ---------- login ----------

    @Test
    void loginWithCorrectPasswordReturnsToken() {
        when(repository.findByUsername("tanaka")).thenReturn(Optional.of(recorder));
        when(jwtTokenService.issue(any(UserAccount.class))).thenReturn("jwt-token-123");

        LoginResponse response = service.login(
                new LoginRequest("tanaka", "Passw0rd!"));

        assertEquals("jwt-token-123", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("WARD_RECORDER", response.getRole());
        assertEquals("Mudzi", response.getWard());
        assertEquals("flood", response.getHazard());
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        when(repository.findByUsername("tanaka")).thenReturn(Optional.of(recorder));

        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("tanaka", "wrong-password")));
    }

    @Test
    void loginWithUnknownUserIsRejected() {
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("ghost", "Passw0rd!")));
    }

    @Test
    void deactivatedAccountCannotLogIn() {
        recorder.setActive(false);
        when(repository.findByUsername("tanaka")).thenReturn(Optional.of(recorder));

        // password is correct, but the account is switched off
        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("tanaka", "Passw0rd!")));
    }

    // ---------- validate ----------

    @Test
    void validateReturnsClaimsOfGoodToken() {
        when(repository.findByUsername("tanaka")).thenReturn(Optional.of(recorder));
        // issue a REAL token with the real JwtTokenService...
        JwtTokenService real = new JwtTokenService(SECRET, 480);
        String token = real.issue(recorder);
        // ...and teach the mocked dependency to verify it exactly like the real one
        when(jwtTokenService.parse(token)).thenReturn(real.parse(token));

        ValidationResponse response = service.validate("Bearer " + token);

        assertTrue(response.isValid());
        assertEquals("tanaka", response.getUsername());
        assertEquals("WARD_RECORDER", response.getRole());
    }

    @Test
    void validateRejectsHeaderWithoutBearerPrefix() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.validate("Basic abcdef"));
    }

    // ---------- createAccount ----------

    @Test
    void createAccountRequiresAdminRole() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newrec").password("Passw0rd!").fullName("New Rec")
                .role(Role.WARD_RECORDER).ward("Mudzi").hazard("fire").build();

        assertThrows(ForbiddenOperationException.class,
                () -> service.createAccount(request, "WARD_RECORDER"));
        verify(repository, never()).save(any());
    }

    @Test
    void adminCreatesRecorderAccountWithHashedPassword() {
        when(repository.existsByUsernameIgnoreCase("newrec")).thenReturn(false);
        when(repository.save(any(UserAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequest request = CreateUserRequest.builder()
                .username("newrec").password("Passw0rd!").fullName("New Rec")
                .role(Role.WARD_RECORDER).ward("Mudzi").hazard("fire").build();
        var response = service.createAccount(request, "PROVINCIAL_ADMIN");

        assertEquals("newrec", response.getUsername());
        assertNotEquals("Passw0rd!", response.toString()); // raw password never leaves the service
    }

    @Test
    void createAccountRejectsWildcardWardForRecorder() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newrec").password("Passw0rd!").fullName("New Rec")
                .role(Role.WARD_RECORDER).ward("ALL").hazard("fire").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createAccount(request, "PROVINCIAL_ADMIN"));
        assertTrue(ex.getMessage().contains("ward"));
        verify(repository, never()).save(any());
    }

    @Test
    void createAccountRejectsBlankHazardForSupervisor() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newsup").password("Passw0rd!").fullName("New Sup")
                .role(Role.PROVINCIAL_SUPERVISOR).hazard(" ").build();

        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount(request, "PROVINCIAL_ADMIN"));
    }

    @Test
    void createAccountRejectsDuplicateUsername() {
        when(repository.existsByUsernameIgnoreCase("tanaka")).thenReturn(true);

        CreateUserRequest request = CreateUserRequest.builder()
                .username("tanaka").password("Passw0rd!").fullName("Dup")
                .role(Role.WARD_RECORDER).ward("Mudzi").hazard("fire").build();

        assertThrows(DuplicateResourceException.class,
                () -> service.createAccount(request, "PROVINCIAL_ADMIN"));
    }

    @Test
    void adminAccountNeedsNoWardOrHazard() {
        when(repository.existsByUsernameIgnoreCase("boss")).thenReturn(false);
        when(repository.save(any(UserAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequest request = CreateUserRequest.builder()
                .username("boss").password("Passw0rd!").fullName("The Admin")
                .role(Role.PROVINCIAL_ADMIN).build(); // no ward/hazard at all

        assertEquals("PROVINCIAL_ADMIN",
                service.createAccount(request, "PROVINCIAL_ADMIN").getRole());
    }

    // ---------- password hashing ----------

    @Test
    void storedPasswordIsBcryptHashNotRawPassword() {
        when(repository.existsByUsernameIgnoreCase("newrec")).thenReturn(false);
        when(repository.save(any(UserAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequest request = CreateUserRequest.builder()
                .username("newrec").password("Passw0rd!").fullName("New Rec")
                .role(Role.WARD_RECORDER).ward("Mudzi").hazard("fire").build();
        service.createAccount(request, "PROVINCIAL_ADMIN");

        // capture what was actually saved
        var saved = org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(saved.capture());
        String hash = saved.getValue().getPasswordHash();
        assertNotEquals("Passw0rd!", hash);
        assertTrue(encoder.matches("Passw0rd!", hash)); // hash verifies against raw password
        assertTrue(hash.startsWith("$2"));              // BCrypt marker
    }
}
