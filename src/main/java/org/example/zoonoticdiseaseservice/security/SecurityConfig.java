package org.example.zoonoticdiseaseservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final AccessControlFilter accessControlFilter;

    public SecurityConfig(AccessControlFilter accessControlFilter) {
        this.accessControlFilter = accessControlFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                .addFilterBefore(
                        accessControlFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}