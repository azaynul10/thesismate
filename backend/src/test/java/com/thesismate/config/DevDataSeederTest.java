package com.thesismate.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.thesismate.repository.AppUserRepository;

class DevDataSeederTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(AppUserRepository.class, () -> mock(AppUserRepository.class))
            .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
            .withUserConfiguration(DevDataSeeder.class);

    @Test
    void seederIsDisabledWithoutDevProfile() {
        runner.run(ctx -> assertFalse(ctx.containsBean("devDataSeeder")));
        runner.withPropertyValues("thesismate.seed=true")
                .run(ctx -> assertFalse(ctx.containsBean("devDataSeeder")));
    }

    @Test
    void seederLoadsOnlyWithDevProfileAndSeedTrue() {
        runner.withPropertyValues("spring.profiles.active=dev", "thesismate.seed=true")
                .run(ctx -> assertTrue(ctx.containsBean("devDataSeeder")));
        runner.withPropertyValues("spring.profiles.active=dev", "thesismate.seed=false")
                .run(ctx -> assertFalse(ctx.containsBean("devDataSeeder")));
    }
}
