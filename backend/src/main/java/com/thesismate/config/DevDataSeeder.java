package com.thesismate.config;

import com.thesismate.model.AppUser;
import com.thesismate.model.Role;
import com.thesismate.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates test accounts for development. Safe to run repeatedly.
 * Runs only with the "dev" profile AND thesismate.seed=true, so these fixed-password
 * accounts can never be created by a normal (non-dev) start-up.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "thesismate.seed", havingValue = "true")
public class DevDataSeeder implements CommandLineRunner {

    private static final String DEV_PASSWORD = "password123";

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public DevDataSeeder(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        create("coordinator@thesismate.test", "Dr. Matin Saad Abdullah", Role.COORDINATOR, null);
        create("chairperson@thesismate.test", "Dr. Sadia Hamid Kazi", Role.CHAIRPERSON, null);
        create("evaluator@thesismate.test", "Dr. Amitabha Chakrabarty", Role.EVALUATOR, null);
        create("supervisor1@thesismate.test", "Dr. Md. Golam Rabiul Alam", Role.SUPERVISOR, null);
        create("supervisor2@thesismate.test", "Dr. Swakkhar Shatabda", Role.SUPERVISOR, null);
        create("supervisor3@thesismate.test", "Dr. Md Sadek Ferdous", Role.SUPERVISOR, null);
        for (int i = 1; i <= 10; i++) {
            create("student" + i + "@thesismate.test", "Student " + i, Role.STUDENT, String.valueOf(20000000 + i));
        }
    }

    private void create(String email, String name, Role role, String studentId) {
        var existing = users.findByEmail(email);
        if (existing.isEmpty()) {
            users.save(new AppUser(email, name, role, encoder.encode(DEV_PASSWORD), studentId));
        } else if (!name.equals(existing.get().getFullName())) {
            AppUser user = existing.get();
            user.setFullName(name);
            users.save(user);
        }
    }
}