package com.lab.atlasmentor.service;

import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.repository.RoleTaskCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates Jira-style display IDs (e.g. "ADM-42") for tasks - one independent, gapless-per-role
 * sequence per {@link Role#getCode()} (see V6__add_jira_style_task_ids.sql / role_task_counters).
 *
 * <p>The counter bump ({@link RoleTaskCounterRepository#incrementAndGetNextSequence}) is a single
 * atomic {@code INSERT ... ON CONFLICT ... DO UPDATE ... RETURNING} statement, so concurrent task
 * creation for the same role can never hand out the same sequence number twice - no separate
 * read-then-write race window to guard against.
 */
@Service
@RequiredArgsConstructor
public class TaskDisplayIdService {

    private final RoleTaskCounterRepository roleTaskCounterRepository;

    /**
     * @param role the role whose counter to advance - for manual tasks this is the assignee's
     *             role, for template/bundle-generated tasks it's the owning bundle's role.
     * @return "{roleCode}-{n}", or null if the role has no code configured (e.g. STUDENT, which
     *         never gets tasks) - callers should tolerate a null/blank displayId in that case.
     */
    @Transactional
    public String nextDisplayId(Role role) {
        if (role == null || role.getCode() == null || role.getCode().isBlank()) {
            return null;
        }
        Long seq = roleTaskCounterRepository.incrementAndGetNextSequence(role.getId());
        return role.getCode() + "-" + seq;
    }
}
