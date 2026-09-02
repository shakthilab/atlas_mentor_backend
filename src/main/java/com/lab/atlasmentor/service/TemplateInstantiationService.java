package com.lab.atlasmentor.service;

import com.lab.atlasmentor.enums.TaskAction;
import com.lab.atlasmentor.enums.TaskSource;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.model.DayWorkspace;
import com.lab.atlasmentor.model.Task;
import com.lab.atlasmentor.model.TaskActivity;
import com.lab.atlasmentor.model.TaskBundle;
import com.lab.atlasmentor.model.TemplateDay;
import com.lab.atlasmentor.model.TemplateTask;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.DayWorkspaceRepository;
import com.lab.atlasmentor.repository.TaskActivityRepository;
import com.lab.atlasmentor.repository.TaskRepository;
import com.lab.atlasmentor.repository.TemplateDayRepository;
import com.lab.atlasmentor.enums.BundleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Single source of truth for turning one day of a Role Template into real,
 * assigned {@link Task} rows for one employee. Every trigger that can kick off
 * instantiation - the nightly cron, the startup catch-up check, and the periodic
 * safety-net check (all three in {@link TemplateInstantiationSchedulerService}), plus
 * the Publish action ({@link TemplatePublishService}) - calls {@link #instantiateDayFor};
 * none of them re-implements the day-number/idempotency/exhaustion rules itself. With
 * several triggers now able to race each other for the same employee+day, the
 * DataIntegrityViolationException handling in {@link #instantiateDayFor} (backed by the
 * uk_day_workspaces_employee_date and uk_tasks_day_workspace_source_template_task DB
 * constraints) is what actually makes that safe, not just the pre-check SELECT.
 *
 * <p>The "day number" looked up in the template is the calendar day-of-month of {@code today}
 * (matching how templates are authored - "Aug 1", "Aug 2", ... "Aug 31" - and mirrored by
 * {@link DayWorkspace#getDayNumberInMonth()}), NOT a per-employee relative counter. Two
 * employees who started on different dates must land on the same {@link TemplateDay} for the
 * same calendar date. (An earlier version of this method derived the day number from how many
 * {@link DayWorkspace} rows the employee already had - that silently mapped every employee's
 * first-ever run to TemplateDay 1 regardless of the actual date, which is a distinct bug from
 * the DataIntegrityViolationException race this class also guards against.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateInstantiationService {

    /** Matches the SYSTEM_USER_ID/ADMIN_USER_ID convention used by the other auto-generation services. */
    private static final Long SYSTEM_USER_ID = 1L;

    private final TemplateDayRepository templateDayRepository;
    private final DayWorkspaceRepository dayWorkspaceRepository;
    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final TaskDisplayIdService taskDisplayIdService;

    /**
     * Instantiate a single day's worth of tasks for one employee under one template, for
     * exactly the given date. Callers control "which date" - this method never looks past it
     * and never walks backwards, so it can't backfill on its own.
     *
     * @param triggerSource who's calling ("cron", "publish", "startup-catch-up",
     *                       "periodic-safety-net", ...) - carried into every log line below
     *                       purely for observability, so it's possible to tell after the fact
     *                       which trigger actually produced (or raced for) a given day's rows.
     *                       Never affects behavior.
     *
     * REQUIRES_NEW: each employee is its own unit of work, so one employee's failure (or a
     * unique-constraint race against a concurrent run - see the DataIntegrityViolationException
     * catch below) can't roll back siblings already processed in the same batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InstantiationResult instantiateDayFor(TaskBundle template, User employee, LocalDate today, String triggerSource) {
        // Idempotency check first: a workspace already on this date means this employee's
        // day was already instantiated (by an earlier cron run, or by Publish earlier today).
        // This is a plain check-then-create and is NOT by itself race-safe against a second
        // trigger running concurrently - the DataIntegrityViolationException catch below is
        // what actually closes that gap, at the DB constraint level. This SELECT just avoids
        // paying for a doomed insert (and its exception) in the overwhelmingly common case
        // where nothing is racing.
        Optional<DayWorkspace> existing = dayWorkspaceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today);
        if (existing.isPresent()) {
            log.debug("[{}] Employee {} already has a day workspace for {}, skipping", triggerSource, employee.getId(), today);
            return InstantiationResult.alreadyInstantiated(employee.getId());
        }

        // Calendar-date-anchored, NOT a per-employee relative counter: TemplateDay.dayNumber
        // lines up 1:1 with the calendar day-of-month (templates are authored as "Aug 1",
        // "Aug 2", ... "Aug 31" - see DayWorkspace#dayNumberInMonth, which this value feeds
        // directly). Two employees starting on different dates must land on the SAME
        // TemplateDay for the SAME calendar date, so this can never be derived from how many
        // DayWorkspace rows an employee happens to already have.
        int dayNumber = today.getDayOfMonth();

        // A day can now be scoped to one specific calendar month (month/year both set) or
        // recurring across every month (month/year both null - the legacy/default shape).
        // Prefer an exact match for today's actual month/year; fall back to the recurring
        // row so templates that have never been given a month-specific override keep
        // instantiating exactly as before.
        List<TemplateDay> templateDays = templateDayRepository.findByRoleTemplateId(template.getId());
        int todayMonth = today.getMonthValue();
        int todayYear = today.getYear();
        Optional<TemplateDay> matchingDay = templateDays.stream()
                .filter(d -> Objects.equals(d.getDayNumber(), dayNumber)
                        && Objects.equals(d.getMonth(), todayMonth)
                        && Objects.equals(d.getYear(), todayYear))
                .findFirst();
        if (matchingDay.isEmpty()) {
            matchingDay = templateDays.stream()
                    .filter(d -> Objects.equals(d.getDayNumber(), dayNumber)
                            && d.getMonth() == null && d.getYear() == null)
                    .findFirst();
        }

        // No TemplateDay at all for this day number: the employee has run past the end of
        // the template, so the cycle really is complete. Distinct from - and NOT to be
        // conflated with - a TemplateDay that exists but simply has no tasks attached yet
        // (handled below): that's a valid no-op day, not the end of the template.
        if (matchingDay.isEmpty()) {
            log.debug("[{}] Employee {} is on day {} but template {} has no day defined there - cycle complete",
                    triggerSource, employee.getId(), dayNumber, template.getId());
            return InstantiationResult.templateExhausted(employee.getId(), dayNumber);
        }

        TemplateDay templateDay = matchingDay.get();
        List<TemplateTask> templateTasks = templateDay.getTemplateTasks();
        boolean dayHasNoTasks = templateTasks == null || templateTasks.isEmpty();

        if (dayHasNoTasks) {
            // Valid, expected state (e.g. a deliberate rest day, or content not authored
            // yet) - not an error. A DayWorkspace still gets created below so the
            // employee's day counter (derived from DayWorkspace count) advances past it;
            // skipping entirely here would leave them stuck re-attempting this same day
            // number forever.
            log.info("[{}] Employee {} day {} of template {} has no tasks defined - creating an "
                            + "empty workspace so their day counter advances",
                    triggerSource, employee.getId(), dayNumber, template.getId());
        }

        try {
            DayWorkspace workspace = new DayWorkspace();
            workspace.setEmployee(employee);
            workspace.setBranch(employee.getBranch());
            workspace.setWorkDate(today);
            workspace.setDayNumberInMonth(dayNumber);
            workspace.setApprovalStage("EMPLOYEE");
            workspace.setIsWeeklyCheckpointDay(Boolean.TRUE.equals(templateDay.getIsWeeklyCheckpoint()));
            workspace.setStreakCount(0);
            if (dayHasNoTasks) {
                // Nothing to complete on a zero-task day, so it reads as fully done rather
                // than as an incomplete/failed 0%.
                workspace.setDailyCompletionPct(BigDecimal.valueOf(100));
            }
            workspace.setCreatedBy(SYSTEM_USER_ID);
            workspace.setUpdatedBy(SYSTEM_USER_ID);
            workspace = dayWorkspaceRepository.save(workspace);

            // FK-only reference, mirrors TaskBundle#setRoleId/Task#setBranchId elsewhere in
            // this codebase: avoids a round trip just to attach the system user by ID.
            User systemUser = new User();
            systemUser.setId(SYSTEM_USER_ID);

            int tasksCreated = 0;
            if (!dayHasNoTasks) {
                for (TemplateTask templateTask : templateTasks) {
                    Task task = new Task();
                    task.setTitle(templateTask.getTitle());
                    task.setDescription(templateTask.getDescription());
                    task.setAssignedTo(employee);
                    task.setAssignedBy(systemUser);
                    task.setCreatedByUser(systemUser);
                    task.setPriority(templateTask.getPriority());
                    // Snapshot, not a live link (V35) - later edits to the template's
                    // proofRequired never retroactively affect this already-instantiated task.
                    task.setProofRequired(templateTask.getProofRequired());
                    // Every task now starts at TODO - PENDING (which used to duplicate this
                    // "not started" state under a separate, stricter transition rule) was
                    // retired; see TaskService#validateStatusTransition's javadoc.
                    task.setStatus(TaskStatus.TODO);
                    task.setTaskBundle(template);
                    task.setSourceType(TaskSource.TEMPLATE_GENERATED);
                    task.setSourceTemplateTask(templateTask);
                    task.setDisplayId(taskDisplayIdService.nextDisplayId(template.getRole()));
                    task.setExecutionDate(today);
                    task.setDueDate(today);
                    task.setDayWorkspace(workspace);
                    task.setIsDeleted(false);
                    // current_step/next_step (V18) - workspace is freshly created at EMPLOYEE,
                    // so this stamps "Not Submitted" / "Submitted".
                    DayApprovalService.applyStepLabels(task);
                    if (employee.getBranch() != null) {
                        task.setBranch(employee.getBranch());
                    }
                    task.setCreatedBy(SYSTEM_USER_ID);
                    task.setUpdatedBy(SYSTEM_USER_ID);
                    Task savedTask = taskRepository.save(task);

                    // Matches TaskService#createTask (CREATED) / TaskGenerationService
                    // (AUTO_GENERATED) - every other task-creation path logs an initial
                    // activity entry; this one hadn't, so template-instantiated tasks showed
                    // no activity history at all.
                    TaskActivity activity = new TaskActivity(savedTask, TaskAction.AUTO_GENERATED, null, null, systemUser);
                    activity.setCreatedBy(SYSTEM_USER_ID);
                    activity.setUpdatedBy(SYSTEM_USER_ID);
                    taskActivityRepository.save(activity);

                    tasksCreated++;
                }
            }

            log.info("[{}] Instantiated day {} of template {} for employee {}: {} tasks created",
                    triggerSource, dayNumber, template.getId(), employee.getId(), tasksCreated);
            return InstantiationResult.created(employee.getId(), dayNumber, tasksCreated);
        } catch (DataIntegrityViolationException e) {
            // Lost a race against a concurrent run of another trigger (cron, Publish,
            // startup catch-up, or the periodic safety-net can all call this method for the
            // same employee+day) - either uk_day_workspaces_employee_date (the workspace
            // itself already exists) or uk_tasks_day_workspace_source_template_task (a task
            // for this template task on this workspace already exists) fired. Either way the
            // other run already did this work first: this is a duplicate attempt, not an
            // error, so it's logged at info and treated as an ordinary skip.
            log.info("[{}] Duplicate instantiation attempt caught for employee {} on {} (another "
                            + "trigger got there first) - treating as already instantiated",
                    triggerSource, employee.getId(), today);
            return InstantiationResult.alreadyInstantiated(employee.getId());
        }
    }

    /**
     * Outcome of one employee's instantiation attempt, for callers to aggregate into a
     * run summary.
     */
    public static final class InstantiationResult {

        public enum Outcome {
            CREATED,
            ALREADY_INSTANTIATED,
            TEMPLATE_EXHAUSTED
        }

        private final Outcome outcome;
        private final Long employeeId;
        private final Integer dayNumber;
        private final int tasksCreated;

        private InstantiationResult(Outcome outcome, Long employeeId, Integer dayNumber, int tasksCreated) {
            this.outcome = outcome;
            this.employeeId = employeeId;
            this.dayNumber = dayNumber;
            this.tasksCreated = tasksCreated;
        }

        static InstantiationResult created(Long employeeId, int dayNumber, int tasksCreated) {
            return new InstantiationResult(Outcome.CREATED, employeeId, dayNumber, tasksCreated);
        }

        static InstantiationResult alreadyInstantiated(Long employeeId) {
            return new InstantiationResult(Outcome.ALREADY_INSTANTIATED, employeeId, null, 0);
        }

        static InstantiationResult templateExhausted(Long employeeId, int dayNumber) {
            return new InstantiationResult(Outcome.TEMPLATE_EXHAUSTED, employeeId, dayNumber, 0);
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public Integer getDayNumber() {
            return dayNumber;
        }

        public int getTasksCreated() {
            return tasksCreated;
        }
    }
}
