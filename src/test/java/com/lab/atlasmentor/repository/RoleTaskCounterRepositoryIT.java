package com.lab.atlasmentor.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies incrementAndGetNextSequence's INSERT ... ON CONFLICT ... RETURNING actually surfaces
 * the returned value through Spring Data JPA's @Modifying mapping (not just that the SQL is
 * valid Postgres) - this is the one part of TaskDisplayIdService that can't be checked by
 * compilation alone. @Transactional rolls the counter bump back so repeated runs don't drift
 * role_task_counters in whatever DB this runs against.
 */
@SpringBootTest
@Transactional
class RoleTaskCounterRepositoryIT {

    @Autowired
    private RoleTaskCounterRepository roleTaskCounterRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void incrementAndGetNextSequence_returnsSequentialValues() {
        // Role 1 = ADM per V6's seed data; existing value doesn't matter, only that it advances by 1 each call.
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles LIMIT 1", Long.class);

        Long first = roleTaskCounterRepository.incrementAndGetNextSequence(roleId);
        Long second = roleTaskCounterRepository.incrementAndGetNextSequence(roleId);

        assertEquals(first + 1, second, "counter should advance by exactly 1 per call");
    }
}
