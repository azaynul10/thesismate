package com.thesismate.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One shared clock, so date rules (overdue tasks, missing weeks) can be tested with a fixed time.
 * Uses the server's time zone, the same as the LocalDateTime.now() calls in the submission window rules.
 */
@Configuration
public class ClockConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
