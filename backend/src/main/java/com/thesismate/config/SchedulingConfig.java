package com.thesismate.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled jobs (the F17 weekly report reminder; later features can add theirs).
 * Set thesismate.scheduling.enabled=false to switch every scheduled job off, for example in a test.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "thesismate.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
