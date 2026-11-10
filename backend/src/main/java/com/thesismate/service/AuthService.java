package com.thesismate.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.RegisterRequest;
import com.thesismate.dto.UserResponse;
import com.thesismate.model.AppUser;
import com.thesismate.model.Role;
import com.thesismate.repository.AppUserRepository;

/** Business rules for registration and "who am I". */
@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Self-registration creates STUDENT accounts only. */
    public UserResponse register(RegisterRequest req) {
        String email = req.email().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (users.existsByStudentId(req.studentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student ID already registered");
        }
        AppUser saved = users.save(new AppUser(email, req.fullName(), Role.STUDENT,
                encoder.encode(req.password()), req.studentId()));
        return UserResponse.from(saved);
    }

    public UserResponse currentUser(String email) {
        return users.findByEmail(email.toLowerCase())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }
}