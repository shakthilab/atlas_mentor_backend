package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.enums.UserStatus;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.model.WeeklyAccountabilityAssignment;
import com.lab.atlasmentor.model.WeeklyAccountabilityTemplate;
import com.lab.atlasmentor.model.WeeklyAccountabilityWeek;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityAssignmentRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Weekly counterpart to {@link TemplateInstantiationSchedulerService}, following the exact
 * same three-trigger pattern for the same reason (a plain {@code @Scheduled(cron = ...)} only
 * fires if the JVM happens to be alive at that exact second - a late start, a restart, an
 * overnight crash, or a deploy landing on the trigger all silently skip it otherwise):
 *
 * <ol>
 *     <li>{@link #runSaturdayAssignment()} - the primary trigger, cron every Saturday at
 *     00:00.</li>
 *     <li>{@link #runStartupCatchUp()} - runs once when the app finishes starting, closing the
 *     gap if the exact Saturday-midnight moment was missed.</li>
 *     <li>{@link #runPeriodicSafetyNet()} - a lightweight recheck every 30 minutes, self-healing
 *     within the window if the app was already running but the exact-second trigger itself was
 *     missed (GC pause, a deploy landing at 00:00, etc.).</li>
 * </ol>
 *
 * <p><b>This cron is the sole writer of {@link WeeklyAccountabilityAssignment}</b> - the one
 * persisted row per ACTIVE template recording which of its 4 weeks is currently assigned.
 * Unlike the read paths ({@code my-template}, submit), which only ever read that row, this is
 * the only place that decides what the row *should* say, via
 * {@link WeeklyAccountabilityTemplateService#computeSaturdayWeekNumber} - "how many Saturdays
 * of this cycle month have happened as of today", clamped to 4. That calculation is
 * deliberately independent of a week's day_range_start/end: it advances exactly one week per
 * Saturday regardless of which day of the month a given month's Saturdays land on, so a month
 * that doesn't start on a Saturday (most months) assigns on exactly the same cadence as one
 * that does (e.g. August 2026). All three triggers below call the same {@link #runAssignment}
 * with a real calendar date, so a catch-up run on, say, a Tuesday still computes the same week
 * number the following true Saturday would have - it's a function of elapsed Saturdays, not of
 * "did a cron fire".
 *
 * <p>Writing to {@link WeeklyAccountabilityAssignment} only happens when the computed week
 * number actually differs from what's already persisted, so the periodic safety net firing
 * every 30 minutes between Saturdays is a no-op (it recomputes the same week number and skips
 * the write) rather than needlessly bumping {@code assigned_at}/{@code trigger_source} on every
 * tick.
 *
 * <p><b>Saturday vs. "due Friday" - confirmed intentional, no conflict.</b> due_weekday remains
 * a display/reminder label only (see {@link WeeklyAccountabilityResponseService}), never an
 * enforced cutoff, and has no bearing on which week this cron assigns. Skips gracefully - no
 * error, no partial state - when a role has no ACTIVE template for the current cycle month, the
 * cycle month's first Saturday hasn't happened yet, or the week being assigned has zero
 * questions (unscheduled), same as an empty day in the daily task cron.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyAccountabilityAssignmentSchedulerService {

    private final WeeklyAccountabilityTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final WeeklyAccountabilityTemplateService weeklyAccountabilityTemplateService;
    private final WeeklyAccountabilityAssignmentRepository assignmentRepository;

    @Scheduled(cron = "0 0 0 * * SAT")
    @Transactional
    public void runSaturdayAssignment() {
        runAssignment(LocalDate.now(), "cron");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runStartupCatchUp() {
        log.info("Running startup catch-up weekly accountability assignment check");
        runAssignment(LocalDate.now(), "startup-catch-up");
    }

    @Scheduled(fixedRate = 1_800_000) // every 30 minutes
    @Transactional
    public void runPeriodicSafetyNet() {
        runAssignment(LocalDate.now(), "periodic-safety-net");
    }

    public RunSummary runAssignment(LocalDate today, String triggerSource) {
        LocalDate cycleMonth = today.withDayOfMonth(1);
        log.info("[{}] Starting weekly accountability assignment for cycle month {} (today: {})", triggerSource, cycleMonth, today);

        List<WeeklyAccountabilityTemplate> activeTemplates = templateRepository.findByStatus(BundleStatus.ACTIVE).stream()
                .filter(t -> cycleMonth.equals(t.getCycleMonth()))
                .toList();

        RunSummary summary = new RunSummary();
        summary.templatesProcessed = activeTemplates.size();

        for (WeeklyAccountabilityTemplate template : activeTemplates) {
            try {
                processTemplate(template, today, summary, triggerSource);
            } catch (Exception e) {
                summary.errors++;
                log.error("[{}] Error processing weekly accountability template {} ({}): {}",
                        triggerSource, template.getId(), template.getName(), e.getMessage(), e);
            }
        }

        log.info("[{}] Weekly accountability assignment complete for cycle month {}: templatesProcessed={}, "
                        + "weeksAssigned={}, templatesSkippedBeforeFirstSaturday={}, templatesSkippedUnscheduled={}, "
                        + "employeesNotified={}, errors={}",
                triggerSource, cycleMonth, summary.templatesProcessed, summary.weeksAssigned,
                summary.templatesSkippedBeforeFirstSaturday, summary.templatesSkippedUnscheduled,
                summary.employeesNotified, summary.errors);

        return summary;
    }

    private void processTemplate(WeeklyAccountabilityTemplate template, LocalDate today, RunSummary summary, String triggerSource) {
        if (template.getTargetRoleId() == null) {
            log.warn("[{}] Weekly accountability template {} ({}) has no target role, skipping", triggerSource, template.getId(), template.getName());
            return;
        }

        int weekNumber = weeklyAccountabilityTemplateService.computeSaturdayWeekNumber(template.getCycleMonth(), today);
        if (weekNumber < 1) {
            log.debug("[{}] Weekly accountability template {} ({}): cycle month's first Saturday hasn't happened yet as of {}, skipping",
                    triggerSource, template.getId(), template.getName(), today);
            summary.templatesSkippedBeforeFirstSaturday++;
            return;
        }

        Optional<WeeklyAccountabilityWeek> resolvedWeek = weeklyAccountabilityTemplateService.resolveWeekByNumber(template, weekNumber);
        if (resolvedWeek.isEmpty() || !resolvedWeek.get().isScheduled()) {
            log.debug("[{}] Weekly accountability template {} ({}): week {} (Saturday-computed) has no questions scheduled, skipping",
                    triggerSource, template.getId(), template.getName(), weekNumber);
            summary.templatesSkippedUnscheduled++;
            return;
        }
        WeeklyAccountabilityWeek week = resolvedWeek.get();

        boolean assignmentChanged = upsertAssignment(template, week, weekNumber, triggerSource);

        List<User> employees = userRepository.findByRoleIdAndStatus(template.getTargetRoleId(), UserStatus.ACTIVE);

        log.info("[{}] Weekly accountability template {} ({}): Week {} ({}-{}, due {}) {} for {} employee(s) under role {}",
                triggerSource, template.getId(), template.getName(), week.getWeekNumber(), week.getDayRangeStart(),
                week.getDayRangeEnd(), week.getDueWeekday(), assignmentChanged ? "newly assigned" : "already assigned",
                employees.size(), template.getTargetRoleId());

        summary.weeksAssigned++;
        summary.employeesNotified += employees.size();
    }

    /**
     * Upserts the template's single {@link WeeklyAccountabilityAssignment} row - only actually
     * writes when the computed week number differs from what's already persisted, so repeated
     * safety-net ticks within the same week are no-ops (assigned_at/trigger_source stay
     * pinned to whichever run first assigned this week).
     *
     * @return true if this call created or advanced the assignment, false if it was already up to date
     */
    private boolean upsertAssignment(WeeklyAccountabilityTemplate template, WeeklyAccountabilityWeek week, int weekNumber, String triggerSource) {
        Optional<WeeklyAccountabilityAssignment> existing = assignmentRepository.findByTemplate_Id(template.getId());
        if (existing.isPresent() && Objects.equals(existing.get().getWeekNumber(), weekNumber)) {
            return false;
        }

        WeeklyAccountabilityAssignment assignment = existing.orElseGet(WeeklyAccountabilityAssignment::new);
        assignment.setTemplate(template);
        assignment.setWeek(week);
        assignment.setWeekNumber(weekNumber);
        assignment.setCycleMonth(template.getCycleMonth());
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setTriggerSource(triggerSource);
        assignmentRepository.save(assignment);
        return true;
    }

    /** Aggregate counters for one run, logged as a summary and returned for tests/manual triggers. */
    public static final class RunSummary {
        private int templatesProcessed;
        private int weeksAssigned;
        private int templatesSkippedBeforeFirstSaturday;
        private int templatesSkippedUnscheduled;
        private int employeesNotified;
        private int errors;

        public int getTemplatesProcessed() { return templatesProcessed; }
        public int getWeeksAssigned() { return weeksAssigned; }
        public int getTemplatesSkippedBeforeFirstSaturday() { return templatesSkippedBeforeFirstSaturday; }
        public int getTemplatesSkippedUnscheduled() { return templatesSkippedUnscheduled; }
        public int getEmployeesNotified() { return employeesNotified; }
        public int getErrors() { return errors; }
    }
}
