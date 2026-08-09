package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.TaskBundle;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.TaskBundleRepository;
import com.lab.atlasmentor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Orchestrator for {@link TemplateInstantiationService}, with three triggers feeding it:
 *
 * <ol>
 *     <li>{@link #runNightlyInstantiation()} - the primary trigger, cron at 00:00.</li>
 *     <li>{@link #runStartupCatchUp()} - runs once when the app finishes starting. Exists
 *     because {@code @Scheduled(cron = ...)} only fires if the JVM happens to be alive at
 *     exactly 00:00:00: a late start, a restart, an overnight crash, or a mid-day deploy all
 *     silently skip that day's cron with no other trigger until the following midnight. This
 *     closes that gap for *today* only - see the class Javadoc note on backfill below.</li>
 *     <li>{@link #runPeriodicSafetyNet()} - a lightweight recheck every 30 minutes, for the
 *     rarer case where the app was already running before midnight but the exact-second
 *     trigger was itself missed (GC pause, a deploy landing at 00:00, etc.). Self-heals
 *     within the window instead of waiting a full day.</li>
 * </ol>
 *
 * All three call the exact same {@link #instantiateForDate} - which in turn always calls
 * {@link TemplateInstantiationService#instantiateDayFor}, the same method the Publish action
 * uses via {@link TemplatePublishService} - so there is exactly one place the
 * day-number/idempotency/exhaustion rules live, and exactly one place (that method's
 * DataIntegrityViolationException handling, backed by DB-level UNIQUE constraints) making it
 * safe for two of these triggers to fire for the same employee+day at nearly the same time.
 *
 * <p><b>Deliberately not handled here:</b> backfilling days an employee missed entirely in
 * the past (e.g. the app was down for 3 days straight). Catch-up and the safety net only ever
 * instantiate <i>today</i>, the same as the cron - if a real multi-day outage becomes possible
 * given how this is deployed, that needs its own explicit decision (skip forward vs. serve the
 * missed days late vs. flag for manual review), not a silent default here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateInstantiationSchedulerService {

    private final TaskBundleRepository taskBundleRepository;
    private final UserRepository userRepository;
    private final TemplateInstantiationService templateInstantiationService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional(readOnly = true)
    public void runNightlyInstantiation() {
        instantiateForDate(LocalDate.now(), "cron");
    }

    /**
     * Fires once, after the app is fully up. Covers the case where the exact midnight moment
     * was missed entirely (late start, restart, overnight crash, mid-day deploy) by treating
     * "did today's instantiation happen yet?" as a normal startup check rather than something
     * only midnight can answer.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void runStartupCatchUp() {
        log.info("Running startup catch-up template instantiation check");
        instantiateForDate(LocalDate.now(), "startup-catch-up");
    }

    /**
     * Cheap self-heal: re-runs the same "has today been instantiated?" check every 30
     * minutes. A no-op almost every time it fires (the idempotency check short-circuits
     * before touching a template or employee), so the fixed rate can stay short without
     * meaningfully adding load.
     */
    @Scheduled(fixedRate = 1_800_000) // every 30 minutes
    @Transactional(readOnly = true)
    public void runPeriodicSafetyNet() {
        instantiateForDate(LocalDate.now(), "periodic-safety-net");
    }

    /**
     * Not @Transactional itself - each employee's instantiation owns its own REQUIRES_NEW
     * transaction (see TemplateInstantiationService), so one failure can't roll back work
     * already committed for other employees or other templates in the same run.
     */
    public RunSummary instantiateForDate(LocalDate today, String triggerSource) {
        log.info("[{}] Starting template instantiation for {}", triggerSource, today);

        List<TaskBundle> allTemplates = taskBundleRepository.findAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .toList();
        List<TaskBundle> activeTemplates = allTemplates.stream()
                .filter(t -> t.getStatus() == BundleStatus.ACTIVE)
                .toList();
        int skippedTemplates = allTemplates.size() - activeTemplates.size();

        RunSummary summary = new RunSummary();
        summary.templatesProcessed = activeTemplates.size();
        summary.templatesSkippedInactive = skippedTemplates;

        for (TaskBundle template : activeTemplates) {
            try {
                processTemplate(template, today, summary, triggerSource);
            } catch (Exception e) {
                summary.errors++;
                log.error("[{}] Error processing template {} ({}): {}",
                        triggerSource, template.getId(), template.getName(), e.getMessage(), e);
            }
        }

        log.info("[{}] Template instantiation complete for {}: templatesProcessed={}, templatesSkippedInactive={}, "
                        + "employeesProcessed={}, tasksCreated={}, employeesSkippedAlreadyInstantiated={}, "
                        + "employeesSkippedTemplateExhausted={}, errors={}",
                triggerSource, today, summary.templatesProcessed, summary.templatesSkippedInactive, summary.employeesProcessed,
                summary.tasksCreated, summary.employeesSkippedAlreadyInstantiated,
                summary.employeesSkippedTemplateExhausted, summary.errors);

        return summary;
    }

    private void processTemplate(TaskBundle template, LocalDate today, RunSummary summary, String triggerSource) {
        if (template.getRole() == null) {
            log.warn("[{}] Template {} ({}) has no target role, skipping", triggerSource, template.getId(), template.getName());
            return;
        }

        List<User> employees = userRepository.findByRoleIdAndStatus(template.getRole().getId(), UserStatus.ACTIVE);

        for (User employee : employees) {
            summary.employeesProcessed++;
            try {
                TemplateInstantiationService.InstantiationResult result =
                        templateInstantiationService.instantiateDayFor(template, employee, today, triggerSource);
                switch (result.getOutcome()) {
                    case CREATED -> summary.tasksCreated += result.getTasksCreated();
                    case ALREADY_INSTANTIATED -> summary.employeesSkippedAlreadyInstantiated++;
                    case TEMPLATE_EXHAUSTED -> summary.employeesSkippedTemplateExhausted++;
                }
            } catch (Exception e) {
                summary.errors++;
                log.error("[{}] Error instantiating template {} day for employee {}: {}",
                        triggerSource, template.getId(), employee.getId(), e.getMessage(), e);
            }
        }
    }

    /** Aggregate counters for one cron run, logged as a summary and returned for tests/manual triggers. */
    public static final class RunSummary {
        private int templatesProcessed;
        private int templatesSkippedInactive;
        private int employeesProcessed;
        private int tasksCreated;
        private int employeesSkippedAlreadyInstantiated;
        private int employeesSkippedTemplateExhausted;
        private int errors;

        public int getTemplatesProcessed() { return templatesProcessed; }
        public int getTemplatesSkippedInactive() { return templatesSkippedInactive; }
        public int getEmployeesProcessed() { return employeesProcessed; }
        public int getTasksCreated() { return tasksCreated; }
        public int getEmployeesSkippedAlreadyInstantiated() { return employeesSkippedAlreadyInstantiated; }
        public int getEmployeesSkippedTemplateExhausted() { return employeesSkippedTemplateExhausted; }
        public int getErrors() { return errors; }
    }
}
