package com.dpdms.auth_service.bootstrap;

import com.dpdms.auth_service.model.Role;
import com.dpdms.auth_service.model.UserAccount;
import com.dpdms.auth_service.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo accounts on first start so the system is usable immediately
 * (documented in the README). Usernames: admin / *.supervisor / ward*.*
 */
@Component
@RequiredArgsConstructor
public class UserDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataSeeder.class);

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        seed("admin", "Admin@123", "Provincial Administrator", "admin@dpdms.example",
                "+263771000001", Role.PROVINCIAL_ADMIN, null, null);

        seed("flood.supervisor", "Super@123", "Flood Provincial Supervisor", "flood.sup@dpdms.example",
                "+263771000011", Role.PROVINCIAL_SUPERVISOR, null, "flood");
        seed("drought.supervisor", "Super@123", "Drought Provincial Supervisor", "drought.sup@dpdms.example",
                "+263771000012", Role.PROVINCIAL_SUPERVISOR, null, "drought");
        seed("fire.supervisor", "Super@123", "Fire Provincial Supervisor", "fire.sup@dpdms.example",
                "+263771000013", Role.PROVINCIAL_SUPERVISOR, null, "fire");
        seed("zoo.supervisor", "Super@123", "Zoonotic Provincial Supervisor", "zoo.sup@dpdms.example",
                "+263771000014", Role.PROVINCIAL_SUPERVISOR, null, "zoonotic");
        seed("mining.supervisor", "Super@123", "Mining Provincial Supervisor", "mining.sup@dpdms.example",
                "+263771000015", Role.PROVINCIAL_SUPERVISOR, null, "mining");

        seed("ward1.flood", "Ward@123", "Ward 1 Flood Recorder", "ward1@dpdms.example",
                "+263771001001", Role.WARD_RECORDER, "Ward 1", "flood");
        seed("ward3.drought", "Ward@123", "Ward 3 Drought Recorder", "ward3@dpdms.example",
                "+263771001003", Role.WARD_RECORDER, "Ward 3", "drought");
        seed("ward4.fire", "Ward@123", "Ward 4 Fire Recorder", "ward4@dpdms.example",
                "+263771001004", Role.WARD_RECORDER, "Ward 4", "fire");
        seed("ward11.fire", "Ward@123", "Ward 11 Fire Recorder", "ward11@dpdms.example",
                "+263771001011", Role.WARD_RECORDER, "Ward 11", "fire");
        seed("ward12.fire", "Ward@123", "Ward 12 Fire Recorder", "ward12@dpdms.example",
                "+263771001012", Role.WARD_RECORDER, "Ward 12", "fire");
        seed("ward13.fire", "Ward@123", "Ward 13 Fire Recorder", "ward13@dpdms.example",
                "+263771001013", Role.WARD_RECORDER, "Ward 13", "fire");
        seed("ward5.zoo", "Ward@123", "Ward 5 Zoonotic Recorder", "ward5@dpdms.example",
                "+263771001005", Role.WARD_RECORDER, "Ward 5", "zoonotic");
        seed("ward6.mining", "Ward@123", "Ward 6 Mining Recorder", "ward6@dpdms.example",
                "+263771001006", Role.WARD_RECORDER, "Ward 6", "mining");

        log.info("Seeded {} demo accounts (admin / *.supervisor / ward*.*)", users.count());
    }

    private void seed(String username, String password, String fullName, String email, String phone,
                      Role role, String ward, String hazard) {
        users.save(UserAccount.builder()
                .username(username)
                .passwordHash(encoder.encode(password))
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .role(role)
                .ward(ward)
                .hazard(hazard)
                .active(true)
                .build());
    }
}
