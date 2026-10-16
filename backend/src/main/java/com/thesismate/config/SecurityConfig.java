package com.thesismate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Temporary setup: /api/health is public, everything else needs login.
 *  Role rules (STUDENT, SUPERVISOR, COORDINATOR, EVALUATOR) are added in Sprint 1. */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .anyRequest().authenticated())
            .httpBasic(b -> {});
        return http.build();
    }
}
