package com.dpdms.auth_service.bootstrap;

import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds demo accounts on first start so the dashboard is usable immediately.
 * Until now the first admin had to be inserted by hand (see auth-service/README.md).
 *
 * Accounts: admin / *.supervisor / ward*.*  (all BCrypt-hashed).
 */
@Component
@RequiredArgsConstructor
public class UserDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataSeeder.class);

    private final UserAccountRepository users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        seed("admin", "Admin@123", "Provincial Administrator", Role.PROVINCIAL_ADMIN, null, null);
        seed("padmin", "ProvAdmin@123", "Provincial Administrator", Role.PROVINCIAL_ADMIN, null, null);

        for (String hazard : List.of("flood", "drought", "fire", "zoonotic", "mining")) {
            seed(hazard + ".supervisor", "Super@123",
                    cap(hazard) + " Provincial Supervisor", Role.PROVINCIAL_SUPERVISOR, null, hazard);
        }

        seed("ward1.flood", "Ward@123", "Ward 1 Flood Recorder", Role.WARD_RECORDER, "Ward 1", "flood");
        seed("ward3.drought", "Ward@123", "Ward 3 Drought Recorder", Role.WARD_RECORDER, "Ward 3", "drought");
        seed("ward4.fire", "Ward@123", "Ward 4 Fire Recorder", Role.WARD_RECORDER, "Ward 4", "fire");
        seed("ward11.fire", "Ward@123", "Ward 11 Fire Recorder", Role.WARD_RECORDER, "Ward 11", "fire");
        seed("ward12.fire", "Ward@123", "Ward 12 Fire Recorder", Role.WARD_RECORDER, "Ward 12", "fire");
        seed("ward13.fire", "Ward@123", "Ward 13 Fire Recorder", Role.WARD_RECORDER, "Ward 13", "fire");
        seed("ward5.zoo", "Ward@123", "Ward 5 Zoonotic Recorder", Role.WARD_RECORDER, "Ward 5", "zoonotic");
        seed("ward6.mining", "Ward@123", "Ward 6 Mining Recorder", Role.WARD_RECORDER, "Ward 6", "mining");

        log.info("Seeded {} demo accounts (admin / *.supervisor / ward*.*)", users.count());
    }

    private void seed(String username, String password, String fullName,
                      Role role, String ward, String hazard) {
        users.save(UserAccount.builder()
                .username(username)
                .passwordHash(encoder.encode(password))
                .fullName(fullName)
                .role(role)
                .ward(ward)
                .hazard(hazard)
                .active(true)
                .build());
    }

    private String cap(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
