package com.lab.atlasmentor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The single {@link Clock} bean the app wires into anything that needs "now" - e.g.
 * {@link com.lab.atlasmentor.service.WeeklyAccountabilityAssignmentSchedulerService}'s
 * startup-catch-up and periodic-safety-net triggers, which otherwise call
 * {@code LocalDate.now()} directly. Injecting this instead of calling {@code LocalDate.now()}
 * inline lets a test pin "now" to a fixed instant with {@code Clock.fixed(...)}, rather than the
 * test's assertions silently drifting out of sync with the real calendar date the suite happens
 * to run on.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
