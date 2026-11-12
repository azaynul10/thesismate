package com.thesismate.service;

import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
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

    private static final int MAX_BCRYPT_BYTES = 72;

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Self-registration creates STUDENT accounts only. */
    public UserResponse register(RegisterRequest req) {
        String email = req.email().toLowerCase();
        // 72 characters can still be more than 72 bytes when non-ASCII characters are used
        if (req.password().getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 bytes");
        }
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (users.existsByStudentId(req.studentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student ID already registered");
        }
        AppUser saved;
        try {
            saved = users.saveAndFlush(new AppUser(email, req.fullName(), Role.STUDENT,
                    encoder.encode(req.password()), req.studentId()));
        } catch (DataIntegrityViolationException e) {
            // Two requests passed the checks above at the same time; the unique constraint caught the second.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or student ID already registered");
        }
        return UserResponse.from(saved);
    }

    public UserResponse currentUser(String email) {
        return users.findByEmail(email.toLowerCase())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }
}