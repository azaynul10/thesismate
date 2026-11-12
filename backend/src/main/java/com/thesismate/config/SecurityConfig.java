package com.thesismate.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/** Session-cookie login with role-based rules. Feature-specific rules are added per sprint. */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** Stores the CSRF token in a cookie the React app can read (not HttpOnly by design). */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
        return repository;
    }

    /**
     * Plain (non-XOR) token handler with eager loading, so every response carries a fresh
     * XSRF-TOKEN cookie and the SPA can send the raw cookie value back as a header.
     */
    public static CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }

    /**
     * Run by AuthController after a successful login: gives the user a NEW session id
     * (prevents session fixation) and a NEW CSRF token.
     */
    public static SessionAuthenticationStrategy loginSessionStrategy(CsrfTokenRepository csrfTokenRepository) {
        CsrfAuthenticationStrategy csrf = new CsrfAuthenticationStrategy(csrfTokenRepository);
        csrf.setRequestHandler(csrfTokenRequestHandler());
        return new CompositeSessionAuthenticationStrategy(
                List.of(new ChangeSessionIdAuthenticationStrategy(), csrf));
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository) throws Exception {
        http
            // CSRF is ON because login uses a session cookie. The token is sent to the browser in the
            // XSRF-TOKEN cookie and the React client echoes it back in the X-XSRF-TOKEN header.
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfTokenRequestHandler()))
            .authorizeHttpRequests(auth -> auth
                // /error must be public, otherwise every error for a logged-out user turns into 401
                .requestMatchers("/api/health", "/api/auth/register", "/api/auth/login", "/error").permitAll()
                .requestMatchers("/api/coordinator/**").hasRole("COORDINATOR")
                .requestMatchers("/api/supervisor/**").hasRole("SUPERVISOR")
                .requestMatchers("/api/chairperson/**").hasRole("CHAIRPERSON")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}