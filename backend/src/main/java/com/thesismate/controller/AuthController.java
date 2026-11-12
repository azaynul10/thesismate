package com.thesismate.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.config.SecurityConfig;
import com.thesismate.dto.LoginRequest;
import com.thesismate.dto.RegisterRequest;
import com.thesismate.dto.UserResponse;
import com.thesismate.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
    private final SessionAuthenticationStrategy loginSessionStrategy;
    private final CsrfLogoutHandler csrfLogoutHandler;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          CsrfTokenRepository csrfTokenRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.loginSessionStrategy = SecurityConfig.loginSessionStrategy(csrfTokenRepository);
        this.csrfLogoutHandler = new CsrfLogoutHandler(csrfTokenRepository);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken
                    .unauthenticated(request.email().toLowerCase(), request.password()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        // New session id + new CSRF token after login (prevents session fixation).
        loginSessionStrategy.onAuthentication(auth, httpRequest, httpResponse);
        contextRepository.saveContext(context, httpRequest, httpResponse);
        return authService.currentUser(auth.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, auth);
        csrfLogoutHandler.logout(request, response, auth);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return authService.currentUser(auth.getName());
    }
}