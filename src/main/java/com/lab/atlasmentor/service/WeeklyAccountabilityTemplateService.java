package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.WeeklyAccountabilityDuplicateTemplateRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityMyTemplateResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityQuestionRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityQuestionResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityTemplateRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityTemplateResponse;
import com.lab.atlasmentor.dto.WeeklyAccountabilityWeekRequest;
import com.lab.atlasmentor.dto.WeeklyAccountabilityWeekResponse;
import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.exception.ConflictException;
import com.lab.atlasmentor.exception.ResourceNotFoundException;
import com.lab.atlasmentor.exception.ValidationException;
import com.lab.atlasmentor.model.Role;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.model.WeeklyAccountabilityAssignment;
import com.lab.atlasmentor.model.WeeklyAccountabilityQuestion;
import com.lab.atlasmentor.model.WeeklyAccountabilityTemplate;
import com.lab.atlasmentor.model.WeeklyAccountabilityWeek;
import com.lab.atlasmentor.repository.RoleRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityAssignmentRepository;
import com.lab.atlasmentor.repository.WeeklyAccountabilityTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CRUD + publish lifecycle for Weekly Accountability Templates - mirrors
 * {@link RoleTemplateService}'s shape (create/get/list/update/publish/delete), but a template
 * here is role- AND cycle-month-scoped, and holds 4 independently-authored
 * {@link WeeklyAccountabilityWeek} slots rather than a single flat question list.
 *
 * <p><b>Day ranges</b> are always server-computed, never client-supplied: weeks 1-3 are fixed
 * 1-7/8-14/15-21, and week 4 runs 22 through the actual last day of the template's cycleMonth
 * (28-31 depending on the month) - computed once here since cycleMonth is already known at
 * creation time, so every calendar day of the month belongs to exactly one week.
 *
 * <p><b>due_weekday is a display/reminder label only</b>, not an enforced submission deadline -
 * see {@link WeeklyAccountabilityResponseService} for where and why.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyAccountabilityTemplateService {

    private static final int WEEK_COUNT = 4;
    private static final DateTimeFormatter CYCLE_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final WeeklyAccountabilityTemplateRepository templateRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final WeeklyAccountabilityAssignmentRepository assignmentRepository;

    @Transactional
    public WeeklyAccountabilityTemplateResponse createTemplate(WeeklyAccountabilityTemplateRequest request, Long currentUserId) {
        log.info("Creating weekly accountability template: {} for roleId: {}, cycleMonth: {}",
                request.getName(), request.getRoleId(), request.getCycleMonth());

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ValidationException("Role not found with ID: " + request.getRoleId()));
        LocalDate cycleMonth = parseCycleMonth(request.getCycleMonth());

        WeeklyAccountabilityTemplate template = new WeeklyAccountabilityTemplate();
        template.setName(request.getName());
        template.setTargetRole(role);
        template.setCycleMonth(cycleMonth);
        template.setStatus(BundleStatus.DRAFT);
        template.setCreatedBy(currentUserId);
        template.setUpdatedBy(currentUserId);
        template.setWeeks(buildInitialWeeks(template, request.getWeeks(), cycleMonth, currentUserId));

        WeeklyAccountabilityTemplate saved = templateRepository.save(template);
        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public WeeklyAccountabilityTemplateResponse getTemplateById(Long id) {
        log.info("Fetching weekly accountability template by ID: {}", id);
        WeeklyAccountabilityTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly accountability template not found with ID: " + id));
        return convertToResponse(template);
    }

    @Transactional(readOnly = true)
    public List<WeeklyAccountabilityTemplateResponse> listTemplates(Long roleId, BundleStatus status) {
        log.info("Listing weekly accountability templates - roleId: {}, status: {}", roleId, status);
        List<WeeklyAccountabilityTemplate> templates;
        if (roleId != null && status != null) {
            templates = templateRepository.findByTargetRole_IdAndStatus(roleId, status);
        } else if (roleId != null) {
            templates = templateRepository.findByTargetRole_Id(roleId);
        } else if (status != null) {
            templates = templateRepository.findByStatus(status);
        } else {
            templates = templateRepository.findAll();
        }
        return templates.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * Updates template metadata (name/role/cycleMonth) and, per week (matched by weekNumber -
     * a template's 4 week slots always exist and are never individually added/removed),
     * updates dueWeekday and diffs that week's nested question list: an entry with an id
     * updates that question in place, an entry without an id creates a new one, and any
     * existing question in that week not referenced by id in the request is removed - same
     * add/update/remove-by-id-presence convention {@link RoleTemplateService#syncDay} uses for
     * tasks, just scoped per week instead of per template. Reordering is just an updated
     * displayOrder on an existing id.
     */
    @Transactional
    public WeeklyAccountabilityTemplateResponse updateTemplate(Long id, WeeklyAccountabilityTemplateRequest request, Long currentUserId) {
        log.info("Updating weekly accountability template ID: {}", id);
        WeeklyAccountabilityTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly accountability template not found with ID: " + id));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ValidationException("Role not found with ID: " + request.getRoleId()));
        LocalDate cycleMonth = parseCycleMonth(request.getCycleMonth());

        template.setName(request.getName());
        template.setTargetRole(role);
        boolean cycleMonthChanged = !cycleMonth.equals(template.getCycleMonth());
        template.setCycleMonth(cycleMonth);
        template.setUpdatedBy(currentUserId);

        if (template.getWeeks() == null || template.getWeeks().isEmpty()) {
            template.setWeeks(buildInitialWeeks(template, request.getWeeks(), cycleMonth, currentUserId));
        } else {
            Map<Integer, WeeklyAccountabilityWeek> existingWeeksByNumber = template.getWeeks().stream()
                    .collect(Collectors.toMap(WeeklyAccountabilityWeek::getWeekNumber, w -> w));
            Map<Integer, WeeklyAccountabilityWeekRequest> requestWeeksByNumber = new HashMap<>();
            if (request.getWeeks() != null) {
                for (WeeklyAccountabilityWeekRequest wReq : request.getWeeks()) {
                    if (wReq.getWeekNumber() != null) {
                        requestWeeksByNumber.put(wReq.getWeekNumber(), wReq);
                    }
                }
            }

            for (int weekNumber = 1; weekNumber <= WEEK_COUNT; weekNumber++) {
                WeeklyAccountabilityWeek week = existingWeeksByNumber.get(weekNumber);
                if (week == null) {
                    // Defensive: an older template row missing a week slot entirely.
                    week = newWeek(template, weekNumber, cycleMonth);
                    template.getWeeks().add(week);
                } else if (cycleMonthChanged) {
                    int[] range = computeDayRange(cycleMonth, weekNumber);
                    week.setDayRangeStart(range[0]);
                    week.setDayRangeEnd(range[1]);
                }

                WeeklyAccountabilityWeekRequest wReq = requestWeeksByNumber.get(weekNumber);
                if (wReq == null) {
                    continue;
                }
                week.setDueWeekday(wReq.getDueWeekday());
                week.setUpdatedBy(currentUserId);
                syncQuestions(week, wReq.getQuestions(), currentUserId);
            }
        }

        WeeklyAccountabilityTemplate saved = templateRepository.save(template);
        return convertToResponse(saved);
    }

    private void syncQuestions(WeeklyAccountabilityWeek week, List<WeeklyAccountabilityQuestionRequest> requestQuestions, Long currentUserId) {
        if (week.getQuestions() == null) {
            week.setQuestions(new ArrayList<>());
        }

        Map<Long, WeeklyAccountabilityQuestionRequest> requestQuestionsMap = new HashMap<>();
        if (requestQuestions != null) {
            for (WeeklyAccountabilityQuestionRequest qReq : requestQuestions) {
                if (qReq.getId() != null) {
                    requestQuestionsMap.put(qReq.getId(), qReq);
                }
            }
        }

        // Remove existing questions not present (by id) in the request.
        week.getQuestions().removeIf(q -> {
            boolean remove = !requestQuestionsMap.containsKey(q.getId());
            if (remove) {
                q.setWeek(null);
            }
            return remove;
        });

        // Add or update.
        if (requestQuestions != null) {
            for (WeeklyAccountabilityQuestionRequest qReq : requestQuestions) {
                if (qReq.getId() == null) {
                    WeeklyAccountabilityQuestion newQuestion = new WeeklyAccountabilityQuestion();
                    newQuestion.setQuestionText(qReq.getQuestionText());
                    newQuestion.setDisplayOrder(qReq.getDisplayOrder() != null ? qReq.getDisplayOrder() : week.getQuestions().size());
                    newQuestion.setWeek(week);
                    newQuestion.setCreatedBy(currentUserId);
                    newQuestion.setUpdatedBy(currentUserId);
                    week.getQuestions().add(newQuestion);
                } else {
                    WeeklyAccountabilityQuestion existing = week.getQuestions().stream()
                            .filter(q -> Objects.equals(q.getId(), qReq.getId()))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("Question not found in week " + week.getWeekNumber()
                                    + " with ID: " + qReq.getId()));
                    existing.setQuestionText(qReq.getQuestionText());
                    if (qReq.getDisplayOrder() != null) {
                        existing.setDisplayOrder(qReq.getDisplayOrder());
                    }
                    existing.setUpdatedBy(currentUserId);
                }
            }
        }
    }

    /**
     * DRAFT -> ACTIVE. Requires at least one of the template's 4 weeks to have >=1 question -
     * NOT all 4 (a template can go live with only Week 1 scheduled and Weeks 2-4 left "Not
     * scheduled" for the rest of the month; my-template simply returns data: null to affected
     * employees during an unscheduled week - see WeeklyAccountabilityTemplateService#getMyTemplate).
     * The uk_weekly_acct_templates_active_role_month partial unique index is the DB-level
     * backstop for "one ACTIVE template per role + cycle month"; this pre-check exists purely
     * to surface a clean 409 instead of a raw constraint-violation 500.
     */
    @Transactional
    public WeeklyAccountabilityTemplateResponse publishTemplate(Long id, Long currentUserId) {
        log.info("Publishing weekly accountability template ID: {}", id);
        WeeklyAccountabilityTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly accountability template not found with ID: " + id));

        boolean hasAnyScheduledWeek = template.getWeeks() != null
                && template.getWeeks().stream().anyMatch(WeeklyAccountabilityWeek::isScheduled);
        if (!hasAnyScheduledWeek) {
            throw new ValidationException("Cannot publish template. At least one week must have at least one question.");
        }

        checkNoConflictingActiveTemplate(template, "publishing");

        template.setStatus(BundleStatus.ACTIVE);
        template.setUpdatedBy(currentUserId);
        WeeklyAccountabilityTemplate saved = templateRepository.save(template);
        return convertToResponse(saved);
    }

    /**
     * Single activate/deactivate endpoint for an already-published template - mirrors
     * {@link RoleTemplateService#updateTemplateStatus}. DRAFT is intentionally out of scope:
     * getting into ACTIVE the first time is {@link #publishTemplate}'s job (it also validates
     * at least one scheduled week), and there is no supported way back into DRAFT once a
     * template has gone live.
     *
     * <p>Deactivating (ACTIVE -> INACTIVE) only flips the flag - it naturally frees up the
     * (role, cycleMonth) slot the partial unique index enforces, so a different template can
     * then be published/reactivated for that same role + month. Reactivating (INACTIVE ->
     * ACTIVE) re-runs the exact same role+cycleMonth conflict check {@link #publishTemplate}
     * does, since another template may have taken that slot while this one sat inactive.
     * Unlike Role Templates, there is no same-day instantiation to (re-)trigger either way -
     * my-template and submit both resolve the current week live on every call, so flipping
     * this flag has no separate materialization step to redo.
     */
    @Transactional
    public WeeklyAccountabilityTemplateResponse updateTemplateStatus(Long id, BundleStatus targetStatus, Long currentUserId) {
        log.info("Updating weekly accountability template ID: {} status to: {}", id, targetStatus);
        WeeklyAccountabilityTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly accountability template not found with ID: " + id));

        if (targetStatus == BundleStatus.DRAFT) {
            throw new ValidationException("Cannot set status to DRAFT. Templates leave DRAFT only via the publish endpoint and never return to it.");
        }

        if (template.getStatus() == BundleStatus.DRAFT) {
            throw new ValidationException("Template is still in DRAFT. Publish it first via PATCH /api/weekly-accountability-templates/"
                    + id + "/publish before changing its status.");
        }

        if (template.getStatus() == targetStatus) {
            log.info("Weekly accountability template ID: {} is already {}, no-op", id, targetStatus);
            return convertToResponse(template);
        }

        if (targetStatus == BundleStatus.ACTIVE) {
            checkNoConflictingActiveTemplate(template, "reactivating");
        }

        template.setStatus(targetStatus);
        template.setUpdatedBy(currentUserId);
        WeeklyAccountabilityTemplate saved = templateRepository.save(template);
        return convertToResponse(saved);
    }

    /**
     * The uk_weekly_acct_templates_active_role_month partial unique index is the DB-level
     * backstop for "one ACTIVE template per role + cycle month"; this pre-check exists purely
     * to surface a clean 409 instead of a raw constraint-violation 500. Shared by
     * {@link #publishTemplate} and {@link #updateTemplateStatus} (reactivate path) - both are
     * ways a template can newly become ACTIVE.
     */
    private void checkNoConflictingActiveTemplate(WeeklyAccountabilityTemplate template, String action) {
        if (template.getTargetRoleId() == null || template.getCycleMonth() == null) {
            return;
        }
        templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(
                        template.getTargetRoleId(), template.getCycleMonth(), BundleStatus.ACTIVE)
                .filter(active -> !active.getId().equals(template.getId()))
                .ifPresent(active -> {
                    throw new ConflictException("Role already has an ACTIVE weekly accountability template (ID: "
                            + active.getId() + ") for " + CYCLE_MONTH_FORMAT.format(template.getCycleMonth())
                            + ". Deactivate it first before " + action + " this one.");
                });
    }

    /** Blocks only ACTIVE templates (409) - DRAFT and INACTIVE can both be deleted. */
    @Transactional
    public void deleteTemplate(Long id) {
        log.info("Deleting weekly accountability template ID: {}", id);
        WeeklyAccountabilityTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly accountability template not found with ID: " + id));

        if (template.getStatus() == BundleStatus.ACTIVE) {
            throw new ConflictException("Cannot delete an ACTIVE template. Deactivate it first, then delete.");
        }

        templateRepository.delete(template);
    }

    /**
     * Copies {@code sourceWeekNumber}'s questions into {@code targetWeekNumber} within the
     * same template - REPLACES the target week's current questions (a one-time copy-in for an
     * empty/unscheduled week, not an append). Only valid within the same template; copying
     * from a different template/month is out of scope.
     */
    @Transactional
    public WeeklyAccountabilityTemplateResponse duplicateWeek(Long templateId, Integer targetWeekNumber, Integer sourceWeekNumber, Long currentUserId) {
        log.info("Duplicating weekly accountability template {} week {} from week {}", templateId, targetWeekNumber, sourceWeekNumber);
        WeeklyAccountabilityTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly accountability template not found with ID: " + templateId));

        if (Objects.equals(targetWeekNumber, sourceWeekNumber)) {
            throw new ValidationException("sourceWeekNumber must be different from the target week.");
        }

        WeeklyAccountabilityWeek targetWeek = findWeekByNumber(template, targetWeekNumber);
        WeeklyAccountabilityWeek sourceWeek = findWeekByNumber(template, sourceWeekNumber);

        if (targetWeek.getQuestions() == null) {
            targetWeek.setQuestions(new ArrayList<>());
        }
        // Replace, not append.
        targetWeek.getQuestions().clear();

        List<WeeklyAccountabilityQuestion> sourceQuestions = sourceWeek.getQuestions() != null
                ? sourceWeek.getQuestions() : List.of();
        for (WeeklyAccountabilityQuestion source : sourceQuestions) {
            WeeklyAccountabilityQuestion copy = new WeeklyAccountabilityQuestion();
            copy.setQuestionText(source.getQuestionText());
            copy.setDisplayOrder(source.getDisplayOrder());
            copy.setWeek(targetWeek);
            copy.setCreatedBy(currentUserId);
            copy.setUpdatedBy(currentUserId);
            targetWeek.getQuestions().add(copy);
        }
        targetWeek.setUpdatedBy(currentUserId);

        WeeklyAccountabilityTemplate saved = templateRepository.save(template);
        return convertToResponse(saved);
    }

    /**
     * Duplicates an entire template (source and every one of its 4 weeks/questions) into a
     * brand new, fully independent template - separate from {@link #duplicateWeek}, which
     * copies one week's questions into another week within a single existing template. Nothing
     * here reuses that code path or shares an FK back to the source: every week and question
     * copied below is a new entity, so subsequent edits on either template never affect the
     * other. Always starts DRAFT, even when duplicating an already-ACTIVE source - the copy
     * must go through its own explicit publish step (and, per role+cycleMonth uniqueness, can
     * only actually publish once it targets a month that isn't already ACTIVE for this role).
     */
    @Transactional
    public WeeklyAccountabilityTemplateResponse duplicateTemplate(Long templateId, WeeklyAccountabilityDuplicateTemplateRequest request, Long currentUserId) {
        WeeklyAccountabilityTemplate source = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly accountability template not found with ID: " + templateId));

        String newName = request.getNewTemplateName();
        if (newName == null || newName.isBlank()) {
            newName = source.getName() + " Copy";
        }
        LocalDate newCycleMonth = (request.getNewCycleMonth() == null || request.getNewCycleMonth().isBlank())
                ? source.getCycleMonth()
                : parseCycleMonth(request.getNewCycleMonth());
        log.info("Duplicating weekly accountability template ID: {} as '{}' for cycleMonth {}", templateId, newName, newCycleMonth);

        WeeklyAccountabilityTemplate copy = new WeeklyAccountabilityTemplate();
        copy.setName(newName);
        copy.setTargetRole(source.getTargetRole());
        copy.setCycleMonth(newCycleMonth);
        copy.setStatus(BundleStatus.DRAFT);
        copy.setCreatedBy(currentUserId);
        copy.setUpdatedBy(currentUserId);

        List<WeeklyAccountabilityWeek> copiedWeeks = new ArrayList<>();
        if (source.getWeeks() != null) {
            List<WeeklyAccountabilityWeek> sourceWeeks = source.getWeeks().stream()
                    .sorted(Comparator.comparing(WeeklyAccountabilityWeek::getWeekNumber))
                    .collect(Collectors.toList());

            for (WeeklyAccountabilityWeek sourceWeek : sourceWeeks) {
                // Day ranges are re-derived for newCycleMonth, never copied verbatim - week 4's
                // end depends on the target month's actual length, which can differ from the
                // source's (e.g. duplicating a 28-day February template into 31-day August).
                WeeklyAccountabilityWeek weekCopy = newWeek(copy, sourceWeek.getWeekNumber(), newCycleMonth);
                weekCopy.setDueWeekday(sourceWeek.getDueWeekday());
                weekCopy.setCreatedBy(currentUserId);
                weekCopy.setUpdatedBy(currentUserId);

                List<WeeklyAccountabilityQuestion> sourceQuestions = sourceWeek.getQuestions() != null
                        ? sourceWeek.getQuestions() : List.of();
                for (WeeklyAccountabilityQuestion sourceQuestion : sourceQuestions) {
                    WeeklyAccountabilityQuestion questionCopy = new WeeklyAccountabilityQuestion();
                    questionCopy.setQuestionText(sourceQuestion.getQuestionText());
                    questionCopy.setDisplayOrder(sourceQuestion.getDisplayOrder());
                    questionCopy.setWeek(weekCopy);
                    questionCopy.setCreatedBy(currentUserId);
                    questionCopy.setUpdatedBy(currentUserId);
                    weekCopy.getQuestions().add(questionCopy);
                }
                copiedWeeks.add(weekCopy);
            }
        }
        copy.setWeeks(copiedWeeks);

        // Single save: cascade=ALL on template.weeks and week.questions persists the whole
        // new tree (template + weeks + questions) in one flush, same pattern createTemplate
        // already uses.
        WeeklyAccountabilityTemplate saved = templateRepository.save(copy);
        return convertToResponse(saved);
    }

    private WeeklyAccountabilityWeek findWeekByNumber(WeeklyAccountabilityTemplate template, Integer weekNumber) {
        return (template.getWeeks() == null ? List.<WeeklyAccountabilityWeek>of() : template.getWeeks()).stream()
                .filter(w -> Objects.equals(w.getWeekNumber(), weekNumber))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Week " + weekNumber + " does not exist on template " + template.getId()));
    }

    /** Looked up by the employee response endpoint and the weekly cron - both need "the current ACTIVE template for a role + month". */
    @Transactional(readOnly = true)
    public Optional<WeeklyAccountabilityTemplate> findActiveTemplateForRoleAndMonth(Long roleId, LocalDate cycleMonth) {
        if (roleId == null || cycleMonth == null) {
            return Optional.empty();
        }
        return templateRepository.findFirstByTargetRole_IdAndCycleMonthAndStatus(roleId, cycleMonth.withDayOfMonth(1), BundleStatus.ACTIVE);
    }

    /**
     * Which week-number (1-4) a Saturday-cron run on {@code asOfDate} corresponds to for a
     * template whose cycle is {@code cycleMonth} - the sole place "which Saturday is this"
     * arithmetic lives, used only by {@link WeeklyAccountabilityAssignmentSchedulerService} to
     * decide what to persist. Week 1 is assigned on the cycle month's first Saturday, week 2 on
     * the next Saturday, and so on; a 5th Saturday in a month (e.g. a month starting on a
     * Saturday, like August 2026) just re-confirms week 4 rather than overflowing.
     *
     * <p>Deliberately independent of the week's day_range_start/end - those describe *display*
     * boundaries only. Two different months can have their first Saturday fall on any day 1-7
     * of the month (a month starting on a Sunday puts it on day 7; a month starting on a
     * Saturday puts it on day 1) and this still advances one week per Saturday regardless.
     *
     * @return 0 if {@code asOfDate} falls before the cycle month's first Saturday (nothing to
     *         assign yet), else the week number clamped to [1, {@value #WEEK_COUNT}].
     */
    public int computeSaturdayWeekNumber(LocalDate cycleMonth, LocalDate asOfDate) {
        LocalDate monthStart = cycleMonth.withDayOfMonth(1);
        if (asOfDate.isBefore(monthStart)) {
            return 0;
        }
        int daysUntilFirstSaturday = (DayOfWeek.SATURDAY.getValue() - monthStart.getDayOfWeek().getValue() + 7) % 7;
        LocalDate firstSaturday = monthStart.plusDays(daysUntilFirstSaturday);
        if (asOfDate.isBefore(firstSaturday)) {
            return 0;
        }
        long saturdaysElapsed = ChronoUnit.DAYS.between(firstSaturday, asOfDate) / 7 + 1;
        return (int) Math.min(saturdaysElapsed, WEEK_COUNT);
    }

    /** Looks up one of a template's 4 week slots by number - used once the week to assign is already known. */
    private Optional<WeeklyAccountabilityWeek> resolveWeekByNumberOptional(WeeklyAccountabilityTemplate template, int weekNumber) {
        if (template.getWeeks() == null) {
            return Optional.empty();
        }
        return template.getWeeks().stream()
                .filter(w -> Objects.equals(w.getWeekNumber(), weekNumber))
                .findFirst();
    }

    /** Public wrapper of {@link #resolveWeekByNumberOptional} - used by the assignment cron once it has computed a week number. */
    public Optional<WeeklyAccountabilityWeek> resolveWeekByNumber(WeeklyAccountabilityTemplate template, int weekNumber) {
        return resolveWeekByNumberOptional(template, weekNumber);
    }

    /**
     * The read-side counterpart to the assignment cron: whatever week the most recent Saturday
     * run persisted for this template, or empty if no run has happened yet (e.g. the template
     * was published after this month's last Saturday already passed, or before its first one).
     * Deliberately does NOT recompute "current week" from today's date - {@code my-template} and
     * submit both call this so they always agree with each other and with the cron's own log.
     */
    @Transactional(readOnly = true)
    public Optional<WeeklyAccountabilityWeek> getCurrentAssignedWeek(WeeklyAccountabilityTemplate template) {
        if (template == null || template.getId() == null) {
            return Optional.empty();
        }
        return assignmentRepository.findByTemplate_Id(template.getId())
                .map(WeeklyAccountabilityAssignment::getWeekNumber)
                .flatMap(weekNumber -> resolveWeekByNumberOptional(template, weekNumber));
    }

    /**
     * Resolves the caller's own role (from the User row, not the JWT's role-name claim, since
     * this needs the actual role FK id) + the current cycle month (this month) + whichever of
     * the ACTIVE template's 4 weeks {@link #getCurrentAssignedWeek} says is currently assigned,
     * and returns just that week's questions - an employee only ever answers the current week's
     * set, never all 4 at once. Returns {@code null} (not a 404) whenever there's nothing to
     * answer right now: no ACTIVE template for this role this month, no Saturday assignment run
     * has happened yet for it, or the assigned week is unscheduled (0 questions) - all normal,
     * expected states.
     */
    @Transactional(readOnly = true)
    public WeeklyAccountabilityMyTemplateResponse getMyTemplate(Long employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        Long roleId = employee.getRole() != null ? employee.getRole().getId() : null;
        if (roleId == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate cycleMonth = today.withDayOfMonth(1);
        Optional<WeeklyAccountabilityTemplate> activeTemplate = findActiveTemplateForRoleAndMonth(roleId, cycleMonth);
        if (activeTemplate.isEmpty()) {
            return null;
        }

        WeeklyAccountabilityTemplate template = activeTemplate.get();
        Optional<WeeklyAccountabilityWeek> currentWeek = getCurrentAssignedWeek(template);
        if (currentWeek.isEmpty() || !currentWeek.get().isScheduled()) {
            return null;
        }

        WeeklyAccountabilityWeek week = currentWeek.get();
        WeeklyAccountabilityMyTemplateResponse response = new WeeklyAccountabilityMyTemplateResponse();
        response.setTemplateId(template.getId());
        response.setTemplateName(template.getName());
        response.setRoleId(roleId);
        response.setCycleMonth(template.getCycleMonth());
        response.setWeekNumber(week.getWeekNumber());
        response.setDayRangeStart(week.getDayRangeStart());
        response.setDayRangeEnd(week.getDayRangeEnd());
        response.setDueWeekday(week.getDueWeekday());
        response.setQuestions(week.getQuestions().stream()
                .map(this::convertQuestionToResponse)
                .sorted(Comparator.comparing(WeeklyAccountabilityQuestionResponse::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList()));
        return response;
    }

    private LocalDate parseCycleMonth(String cycleMonth) {
        if (cycleMonth == null || cycleMonth.isBlank()) {
            throw new ValidationException("cycleMonth is required (format: yyyy-MM).");
        }
        try {
            YearMonth parsed = YearMonth.parse(cycleMonth.trim(), CYCLE_MONTH_FORMAT);
            return parsed.atDay(1);
        } catch (DateTimeException e) {
            throw new ValidationException("cycleMonth is malformed - expected format yyyy-MM, got: " + cycleMonth);
        }
    }

    /** Weeks 1-3 are fixed 7-day blocks; week 4 runs 22 through the actual last day of cycleMonth. */
    private int[] computeDayRange(LocalDate cycleMonth, int weekNumber) {
        return switch (weekNumber) {
            case 1 -> new int[]{1, 7};
            case 2 -> new int[]{8, 14};
            case 3 -> new int[]{15, 21};
            case 4 -> new int[]{22, YearMonth.from(cycleMonth).lengthOfMonth()};
            default -> throw new ValidationException("weekNumber must be between 1 and 4, got: " + weekNumber);
        };
    }

    private WeeklyAccountabilityWeek newWeek(WeeklyAccountabilityTemplate template, int weekNumber, LocalDate cycleMonth) {
        WeeklyAccountabilityWeek week = new WeeklyAccountabilityWeek();
        week.setTemplate(template);
        week.setWeekNumber(weekNumber);
        int[] range = computeDayRange(cycleMonth, weekNumber);
        week.setDayRangeStart(range[0]);
        week.setDayRangeEnd(range[1]);
        week.setQuestions(new ArrayList<>());
        return week;
    }

    /** All 4 week slots always exist, even ones the request omits entirely (they stay "Not scheduled"). */
    private List<WeeklyAccountabilityWeek> buildInitialWeeks(WeeklyAccountabilityTemplate template, List<WeeklyAccountabilityWeekRequest> requestWeeks,
                                                              LocalDate cycleMonth, Long currentUserId) {
        Map<Integer, WeeklyAccountabilityWeekRequest> requestWeeksByNumber = new HashMap<>();
        if (requestWeeks != null) {
            for (WeeklyAccountabilityWeekRequest wReq : requestWeeks) {
                if (wReq.getWeekNumber() == null) {
                    throw new ValidationException("Each week entry requires a weekNumber.");
                }
                if (wReq.getWeekNumber() < 1 || wReq.getWeekNumber() > WEEK_COUNT) {
                    throw new ValidationException("weekNumber must be between 1 and 4, got: " + wReq.getWeekNumber());
                }
                requestWeeksByNumber.put(wReq.getWeekNumber(), wReq);
            }
        }

        return IntStream.rangeClosed(1, WEEK_COUNT)
                .mapToObj(weekNumber -> {
                    WeeklyAccountabilityWeek week = newWeek(template, weekNumber, cycleMonth);
                    week.setCreatedBy(currentUserId);
                    week.setUpdatedBy(currentUserId);

                    WeeklyAccountabilityWeekRequest wReq = requestWeeksByNumber.get(weekNumber);
                    if (wReq == null) {
                        return week;
                    }
                    week.setDueWeekday(wReq.getDueWeekday());
                    if (wReq.getQuestions() != null) {
                        int order = 0;
                        for (WeeklyAccountabilityQuestionRequest qReq : wReq.getQuestions()) {
                            WeeklyAccountabilityQuestion question = new WeeklyAccountabilityQuestion();
                            question.setQuestionText(qReq.getQuestionText());
                            question.setDisplayOrder(qReq.getDisplayOrder() != null ? qReq.getDisplayOrder() : order);
                            question.setWeek(week);
                            question.setCreatedBy(currentUserId);
                            question.setUpdatedBy(currentUserId);
                            week.getQuestions().add(question);
                            order++;
                        }
                    }
                    return week;
                })
                .collect(Collectors.toList());
    }

    private WeeklyAccountabilityTemplateResponse convertToResponse(WeeklyAccountabilityTemplate template) {
        WeeklyAccountabilityTemplateResponse response = new WeeklyAccountabilityTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setStatus(template.getStatus());
        response.setCycleMonth(template.getCycleMonth());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        response.setCreatedBy(template.getCreatedBy());
        response.setUpdatedBy(template.getUpdatedBy());

        if (template.getTargetRole() != null) {
            response.setRoleId(template.getTargetRole().getId());
            response.setRoleName(template.getTargetRole().getName());
            response.setRoleDisplayName(template.getTargetRole().getDisplayName());
        }

        if (template.getWeeks() != null) {
            List<WeeklyAccountabilityWeekResponse> weekResponses = template.getWeeks().stream()
                    .map(this::convertWeekToResponse)
                    .sorted(Comparator.comparing(WeeklyAccountabilityWeekResponse::getWeekNumber, Comparator.nullsLast(Integer::compareTo)))
                    .collect(Collectors.toList());
            response.setWeeks(weekResponses);
        } else {
            response.setWeeks(new ArrayList<>());
        }

        return response;
    }

    private WeeklyAccountabilityWeekResponse convertWeekToResponse(WeeklyAccountabilityWeek week) {
        WeeklyAccountabilityWeekResponse response = new WeeklyAccountabilityWeekResponse();
        response.setId(week.getId());
        response.setWeekNumber(week.getWeekNumber());
        response.setDayRangeStart(week.getDayRangeStart());
        response.setDayRangeEnd(week.getDayRangeEnd());
        response.setDueWeekday(week.getDueWeekday());
        response.setScheduled(week.isScheduled());
        List<WeeklyAccountabilityQuestionResponse> questionResponses = (week.getQuestions() == null ? List.<WeeklyAccountabilityQuestion>of() : week.getQuestions())
                .stream()
                .map(this::convertQuestionToResponse)
                .sorted(Comparator.comparing(WeeklyAccountabilityQuestionResponse::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        response.setQuestions(questionResponses);
        return response;
    }

    private WeeklyAccountabilityQuestionResponse convertQuestionToResponse(WeeklyAccountabilityQuestion question) {
        WeeklyAccountabilityQuestionResponse response = new WeeklyAccountabilityQuestionResponse();
        response.setId(question.getId());
        response.setQuestionText(question.getQuestionText());
        response.setDisplayOrder(question.getDisplayOrder());
        return response;
    }
}
