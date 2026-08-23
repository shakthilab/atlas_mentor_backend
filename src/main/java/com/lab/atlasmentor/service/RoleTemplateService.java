package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.exception.ConflictException;
import com.lab.atlasmentor.exception.ResourceNotFoundException;
import com.lab.atlasmentor.exception.ValidationException;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleTemplateService {

    private final TaskBundleRepository taskBundleRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final TemplateDayRepository templateDayRepository;
    private final TemplateTaskRepository templateTaskRepository;
    private final TemplatePublishService templatePublishService;

    @Transactional
    public RoleTemplateResponse createTemplate(RoleTemplateRequest request, Long currentUserId) {
        log.info("Creating role template: {} for roleId: {}", request.getName(), request.getRoleId());

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ValidationException("Role not found with ID: " + request.getRoleId()));

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ValidationException("Branch not found with ID: " + request.getBranchId()));
        }

        TaskBundle template = new TaskBundle();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setRole(role);
        template.setBranch(branch);
        template.setStatus(BundleStatus.DRAFT);
        template.setIsDeleted(false);
        template.setCreatedBy(currentUserId);
        template.setUpdatedBy(currentUserId);

        List<TemplateDay> templateDays = new ArrayList<>();
        if (request.getDays() != null) {
            for (RoleTemplateDayRequest dayReq : request.getDays()) {
                TemplateDay day = new TemplateDay();
                day.setDayNumber(dayReq.getDayNumber());
                day.setIsWeeklyCheckpoint(dayReq.getIsWeeklyCheckpoint() != null ? dayReq.getIsWeeklyCheckpoint() : false);
                day.setMonth(dayReq.getMonth());
                day.setYear(dayReq.getYear());
                day.setRoleTemplate(template);
                day.setCreatedBy(currentUserId);
                day.setUpdatedBy(currentUserId);

                List<TemplateTask> templateTasks = new ArrayList<>();
                if (dayReq.getTasks() != null) {
                    for (RoleTemplateTaskRequest taskReq : dayReq.getTasks()) {
                        TemplateTask task = new TemplateTask();
                        task.setTitle(taskReq.getTitle());
                        task.setDescription(taskReq.getDescription());
                        task.setPriority(taskReq.getPriority() != null ? taskReq.getPriority() : Priority.MEDIUM);
                        task.setDisplayOrder(taskReq.getDisplayOrder() != null ? taskReq.getDisplayOrder() : 0);
                        task.setTemplateDay(day);
                        task.setCreatedBy(currentUserId);
                        task.setUpdatedBy(currentUserId);
                        templateTasks.add(task);
                    }
                }
                day.setTemplateTasks(templateTasks);
                templateDays.add(day);
            }
        }
        template.setTemplateDays(templateDays);

        TaskBundle saved = taskBundleRepository.save(template);
        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public RoleTemplateResponse getTemplateById(Long id) {
        log.info("Fetching role template by ID: {}", id);
        TaskBundle template = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + id));

        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new ResourceNotFoundException("Role template not found with ID: " + id);
        }

        return convertToResponse(template);
    }

    @Transactional(readOnly = true)
    public List<RoleTemplateResponse> listTemplates(Long roleId, Long branchId, BundleStatus status) {
        log.info("Listing role templates with filters - roleId: {}, branchId: {}, status: {}", roleId, branchId, status);
        List<TaskBundle> templates = taskBundleRepository.findTemplatesWithFilters(roleId, branchId, status);
        return templates.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleTemplateResponse updateTemplate(Long id, RoleTemplateRequest request, Long currentUserId) {
        log.info("Updating role template ID: {}", id);
        TaskBundle template = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + id));

        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new ResourceNotFoundException("Role template not found with ID: " + id);
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ValidationException("Role not found with ID: " + request.getRoleId()));

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ValidationException("Branch not found with ID: " + request.getBranchId()));
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setRole(role);
        template.setBranch(branch);
        template.setUpdatedBy(currentUserId);

        // Nested update of days.
        // Client-supplied days are reconciled as follows:
        //   - id present            -> update that existing day in place
        //   - id absent + has tasks -> update the day matching that dayNumber if one
        //                              already exists, otherwise create a new day
        //   - id absent + no tasks  -> leave untouched (no create, no delete)
        // Days already on the template that a request entry doesn't resolve to are left
        // as-is; this endpoint never implicitly deletes a day.
        if (template.getTemplateDays() == null) {
            template.setTemplateDays(new ArrayList<>());
        }

        if (request.getDays() != null) {
            for (RoleTemplateDayRequest dayReq : request.getDays()) {
                boolean hasContent = dayReq.getTasks() != null && !dayReq.getTasks().isEmpty();

                if (dayReq.getId() != null) {
                    TemplateDay existingDay = template.getTemplateDays().stream()
                            .filter(d -> d.getId().equals(dayReq.getId()))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("Day not found in template with ID: " + dayReq.getId()));
                    syncDay(existingDay, dayReq, currentUserId);
                } else if (hasContent) {
                    TemplateDay matchedDay = template.getTemplateDays().stream()
                            .filter(d -> Objects.equals(d.getDayNumber(), dayReq.getDayNumber())
                                    && Objects.equals(d.getMonth(), dayReq.getMonth())
                                    && Objects.equals(d.getYear(), dayReq.getYear()))
                            .findFirst()
                            .orElse(null);

                    if (matchedDay != null) {
                        syncDay(matchedDay, dayReq, currentUserId);
                    } else {
                        TemplateDay newDay = new TemplateDay();
                        newDay.setRoleTemplate(template);
                        newDay.setCreatedBy(currentUserId);
                        newDay.setTemplateTasks(new ArrayList<>());
                        template.getTemplateDays().add(newDay);
                        syncDay(newDay, dayReq, currentUserId);
                    }
                }
                // else: no id and no tasks -> nothing to do for this day
            }
        }

        TaskBundle saved = taskBundleRepository.save(template);
        return convertToResponse(saved);
    }

    /**
     * Applies a day-level request onto an existing (or freshly created) TemplateDay,
     * then syncs its tasks: tasks with an id are updated in place, tasks without an id
     * are created, and any existing task not referenced by id in the request is removed.
     */
    private void syncDay(TemplateDay day, RoleTemplateDayRequest dayReq, Long currentUserId) {
        day.setDayNumber(dayReq.getDayNumber());
        day.setIsWeeklyCheckpoint(dayReq.getIsWeeklyCheckpoint() != null ? dayReq.getIsWeeklyCheckpoint() : false);
        day.setMonth(dayReq.getMonth());
        day.setYear(dayReq.getYear());
        day.setUpdatedBy(currentUserId);

        Map<Long, RoleTemplateTaskRequest> requestTasksMap = new HashMap<>();
        if (dayReq.getTasks() != null) {
            for (RoleTemplateTaskRequest taskReq : dayReq.getTasks()) {
                if (taskReq.getId() != null) {
                    requestTasksMap.put(taskReq.getId(), taskReq);
                }
            }
        }

        // Remove existing tasks not present in request
        if (day.getTemplateTasks() != null) {
            day.getTemplateTasks().removeIf(t -> {
                boolean remove = !requestTasksMap.containsKey(t.getId());
                if (remove) {
                    t.setTemplateDay(null);
                }
                return remove;
            });
        } else {
            day.setTemplateTasks(new ArrayList<>());
        }

        // Add or update tasks
        if (dayReq.getTasks() != null) {
            for (RoleTemplateTaskRequest taskReq : dayReq.getTasks()) {
                if (taskReq.getId() == null) {
                    // New Task
                    TemplateTask newTask = new TemplateTask();
                    newTask.setTitle(taskReq.getTitle());
                    newTask.setDescription(taskReq.getDescription());
                    newTask.setPriority(taskReq.getPriority() != null ? taskReq.getPriority() : Priority.MEDIUM);
                    newTask.setDisplayOrder(taskReq.getDisplayOrder() != null ? taskReq.getDisplayOrder() : 0);
                    newTask.setTemplateDay(day);
                    newTask.setCreatedBy(currentUserId);
                    newTask.setUpdatedBy(currentUserId);
                    day.getTemplateTasks().add(newTask);
                } else {
                    // Update Task
                    TemplateTask existingTask = day.getTemplateTasks().stream()
                            .filter(t -> t.getId().equals(taskReq.getId()))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("Task not found in day with ID: " + taskReq.getId()));

                    existingTask.setTitle(taskReq.getTitle());
                    existingTask.setDescription(taskReq.getDescription());
                    existingTask.setPriority(taskReq.getPriority() != null ? taskReq.getPriority() : Priority.MEDIUM);
                    existingTask.setDisplayOrder(taskReq.getDisplayOrder() != null ? taskReq.getDisplayOrder() : 0);
                    existingTask.setUpdatedBy(currentUserId);
                }
            }
        }
    }

    @Transactional
    public RoleTemplateResponse duplicateDayTasks(Long templateId, Long dayId, DuplicateDayRequest request, Long currentUserId) {
        log.info("Duplicating day ID: {} tasks under template ID: {}, mode: {}", dayId, templateId, request.getMode());

        TaskBundle template = taskBundleRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + templateId));

        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new ResourceNotFoundException("Role template not found with ID: " + templateId);
        }

        TemplateDay sourceDay = template.getTemplateDays().stream()
                .filter(d -> d.getId().equals(dayId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Source day not found with ID: " + dayId));

        if (request.getMode().equalsIgnoreCase("SINGLE_DAY")) {
            if (request.getTargetDayNumber() == null) {
                throw new ValidationException("Target day number is required for SINGLE_DAY mode.");
            }
            // Duplicate within the same month/year scope as the source day, so duplicating
            // inside "August" never leaks a copy into another month's day of the same number.
            TemplateDay targetDay = findOrCreateDay(template, request.getTargetDayNumber(), sourceDay.getMonth(), sourceDay.getYear(), currentUserId);
            duplicateTasksToDay(sourceDay, targetDay, currentUserId);
        } else if (request.getMode().equalsIgnoreCase("RANGE")) {
            if (request.getRangeLength() == null || request.getRangeLength() <= 0) {
                throw new ValidationException("Valid range length is required for RANGE mode.");
            }
            int startDayNumber = sourceDay.getDayNumber();
            for (int i = 1; i <= request.getRangeLength(); i++) {
                int targetDayNumber = startDayNumber + i;
                TemplateDay targetDay = findOrCreateDay(template, targetDayNumber, sourceDay.getMonth(), sourceDay.getYear(), currentUserId);
                duplicateTasksToDay(sourceDay, targetDay, currentUserId);
            }
        } else {
            throw new ValidationException("Invalid duplication mode: " + request.getMode());
        }

        TaskBundle saved = taskBundleRepository.save(template);
        return convertToResponse(saved);
    }

    /**
     * Duplicates an entire template (source and every one of its days/tasks) into a brand
     * new, fully independent template. This is deliberately separate from
     * {@link #duplicateDayTasks}: that method copies a day's tasks within a single existing
     * template, while this one clones the whole template tree into a new row. Nothing here
     * reuses that code path or shares an FK back to the source - every day and task copied
     * below is a new entity, so subsequent edits on either template never affect the other.
     */
    @Transactional
    public RoleTemplateResponse duplicateTemplate(Long templateId, DuplicateTemplateRequest request, Long currentUserId) {
        TaskBundle source = taskBundleRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + templateId));

        if (Boolean.TRUE.equals(source.getIsDeleted())) {
            throw new ResourceNotFoundException("Role template not found with ID: " + templateId);
        }

        // No name supplied -> "<source name> Copy" (e.g. "Senior Counsellor Aug" ->
        // "Senior Counsellor Aug Copy") so duplicating never fails just for a missing name.
        String newName = request.getNewTemplateName();
        if (newName == null || newName.isBlank()) {
            newName = source.getName() + " Copy";
        }
        log.info("Duplicating role template ID: {} as '{}'", templateId, newName);

        TaskBundle copy = new TaskBundle();
        copy.setName(newName);
        copy.setDescription(source.getDescription());
        copy.setRole(source.getRole());
        copy.setBranch(source.getBranch());
        // Always starts DRAFT regardless of the source's status - the copy must go through
        // its own explicit publish step even when duplicating an already-ACTIVE template.
        copy.setStatus(BundleStatus.DRAFT);
        copy.setIsDeleted(false);
        copy.setCreatedBy(currentUserId);
        copy.setUpdatedBy(currentUserId);

        List<TemplateDay> copiedDays = new ArrayList<>();
        if (source.getTemplateDays() != null) {
            List<TemplateDay> sourceDays = source.getTemplateDays().stream()
                    .sorted(Comparator.comparing(TemplateDay::getDayNumber)
                            .thenComparing(TemplateDay::getYear, Comparator.nullsFirst(Integer::compareTo))
                            .thenComparing(TemplateDay::getMonth, Comparator.nullsFirst(Integer::compareTo)))
                    .collect(Collectors.toList());

            for (TemplateDay sourceDay : sourceDays) {
                TemplateDay dayCopy = new TemplateDay();
                dayCopy.setDayNumber(sourceDay.getDayNumber());
                dayCopy.setIsWeeklyCheckpoint(sourceDay.getIsWeeklyCheckpoint());
                dayCopy.setMonth(sourceDay.getMonth());
                dayCopy.setYear(sourceDay.getYear());
                dayCopy.setRoleTemplate(copy);
                dayCopy.setCreatedBy(currentUserId);
                dayCopy.setUpdatedBy(currentUserId);

                List<TemplateTask> copiedTasks = new ArrayList<>();
                if (sourceDay.getTemplateTasks() != null) {
                    List<TemplateTask> sourceTasks = sourceDay.getTemplateTasks().stream()
                            .sorted(Comparator.comparing(TemplateTask::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                            .collect(Collectors.toList());

                    for (TemplateTask sourceTask : sourceTasks) {
                        TemplateTask taskCopy = new TemplateTask();
                        taskCopy.setTitle(sourceTask.getTitle());
                        taskCopy.setDescription(sourceTask.getDescription());
                        taskCopy.setPriority(sourceTask.getPriority());
                        taskCopy.setDisplayOrder(sourceTask.getDisplayOrder());
                        taskCopy.setTemplateDay(dayCopy);
                        taskCopy.setCreatedBy(currentUserId);
                        taskCopy.setUpdatedBy(currentUserId);
                        copiedTasks.add(taskCopy);
                    }
                }
                dayCopy.setTemplateTasks(copiedTasks);
                copiedDays.add(dayCopy);
            }
        }
        copy.setTemplateDays(copiedDays);

        // Single save: cascade=ALL on TaskBundle.templateDays and TemplateDay.templateTasks
        // persists the whole new tree (template + days + tasks) in one flush, same pattern
        // createTemplate already uses. Name uniqueness is likewise left to the DB's
        // uk_task_bundles_name_role constraint / GlobalExceptionHandler 409, exactly as
        // createTemplate does - duplicate names are rejected only when they collide with
        // the same role, not globally.
        TaskBundle saved = taskBundleRepository.save(copy);
        return convertToResponse(saved);
    }

    @Transactional
    public RoleTemplateResponse publishTemplate(Long id, Long currentUserId) {
        log.info("Publishing role template ID: {}", id);
        TaskBundle template = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + id));

        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new ResourceNotFoundException("Role template not found with ID: " + id);
        }

        boolean hasTask = false;
        if (template.getTemplateDays() != null && !template.getTemplateDays().isEmpty()) {
            for (TemplateDay day : template.getTemplateDays()) {
                if (day.getTemplateTasks() != null && !day.getTemplateTasks().isEmpty()) {
                    hasTask = true;
                    break;
                }
            }
        }

        if (!hasTask) {
            throw new ValidationException("Cannot publish template. It must have at least one day with at least one task.");
        }

        template.setStatus(BundleStatus.ACTIVE);
        template.setUpdatedBy(currentUserId);
        TaskBundle saved = taskBundleRepository.save(template);

        templatePublishService.instantiateFor(saved);

        return convertToResponse(saved);
    }

    /**
     * Single activate/deactivate endpoint for an already-published template. DRAFT is
     * intentionally out of scope here: getting into ACTIVE the first time is the
     * dedicated {@link #publishTemplate}'s job (it also validates the template has at
     * least one task and triggers today's instantiation), and there is no supported way
     * back into DRAFT once a template has gone live.
     *
     * Reactivating (INACTIVE -> ACTIVE) runs the exact same immediate same-day
     * instantiation as publish, via the same {@link TemplatePublishService} call - so an
     * employee whose Day N was missed while the template sat inactive doesn't have to wait
     * for tonight's cron. Deactivating (ACTIVE -> INACTIVE) only flips the flag: the nightly
     * cron already filters to ACTIVE-only, so that alone stops future instantiation, and
     * per the existing "template changes never cascade" rule, already-assigned tasks are
     * left untouched either way.
     */
    @Transactional
    public RoleTemplateResponse updateTemplateStatus(Long id, BundleStatus targetStatus, Long currentUserId) {
        log.info("Updating role template ID: {} status to: {}", id, targetStatus);
        TaskBundle template = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + id));

        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new ResourceNotFoundException("Role template not found with ID: " + id);
        }

        if (targetStatus == BundleStatus.DRAFT) {
            throw new ValidationException("Cannot set status to DRAFT. Templates leave DRAFT only via the publish endpoint and never return to it.");
        }

        if (template.getStatus() == BundleStatus.DRAFT) {
            throw new ValidationException("Template is still in DRAFT. Publish it first via PATCH /api/role-templates/" + id + "/publish before changing its status.");
        }

        if (template.getStatus() == targetStatus) {
            log.info("Role template ID: {} is already {}, no-op", id, targetStatus);
            return convertToResponse(template);
        }

        template.setStatus(targetStatus);
        template.setUpdatedBy(currentUserId);
        TaskBundle saved = taskBundleRepository.save(template);

        if (targetStatus == BundleStatus.ACTIVE) {
            templatePublishService.instantiateFor(saved);
        }

        return convertToResponse(saved);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        log.info("Deleting role template ID: {}", id);
        TaskBundle template = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + id));

        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            return;
        }

        if (template.getStatus() == BundleStatus.ACTIVE) {
            throw new ConflictException("Cannot delete an ACTIVE template. Only templates in DRAFT status can be deleted.");
        }

        template.setIsDeleted(true);
        taskBundleRepository.save(template);
    }

    /**
     * Permanently removes a template row (and, via cascade = ALL/orphanRemoval on
     * TaskBundle -> TemplateDay -> TemplateTask, every day and task under it) from the
     * database. Unlike {@link #deleteTemplate}, which only flips is_deleted, this leaves
     * nothing behind and cannot be undone.
     *
     * Deliberately runs the same "not ACTIVE" guard as the soft delete, since an ACTIVE
     * template is expected to be deactivated first. If real employee tasks were already
     * instantiated from this template while it was ACTIVE (task_bundle_id on task rows),
     * the DB FK blocks the delete and it surfaces as the standard 409 data-conflict
     * response rather than silently orphaning those rows - deactivate/reassign first in
     * that case.
     */
    @Transactional
    public void hardDeleteTemplate(Long id) {
        log.info("Hard deleting role template ID: {}", id);
        TaskBundle template = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + id));

        if (template.getStatus() == BundleStatus.ACTIVE) {
            throw new ConflictException("Cannot delete an ACTIVE template. Deactivate it first, then delete.");
        }

        taskBundleRepository.delete(template);
    }

    /**
     * Resolves the TemplateDay for a given dayNumber, optionally scoped to one calendar
     * month. When month/year are supplied, an exact (dayNumber, month, year) match is
     * preferred; if none exists yet, one is created rather than falling back to the
     * recurring row - this is what makes edits made while viewing a specific month (e.g.
     * "August 2026, Day 22") independent of every other month's Day 22 instead of silently
     * mutating the shared recurring day.
     * When month/year are both null, resolves (and creates, if missing) the recurring row
     * that applies to this dayNumber in every month - the legacy/default behavior.
     */
    private TemplateDay findOrCreateDay(TaskBundle template, int dayNumber, Integer month, Integer year, Long currentUserId) {
        Optional<TemplateDay> existing = template.getTemplateDays().stream()
                .filter(d -> d.getDayNumber() == dayNumber
                        && Objects.equals(d.getMonth(), month)
                        && Objects.equals(d.getYear(), year))
                .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        TemplateDay newDay = new TemplateDay();
        newDay.setDayNumber(dayNumber);
        newDay.setIsWeeklyCheckpoint(false); // default to false, can be updated later
        newDay.setMonth(month);
        newDay.setYear(year);
        newDay.setRoleTemplate(template);
        newDay.setCreatedBy(currentUserId);
        newDay.setUpdatedBy(currentUserId);
        newDay.setTemplateTasks(new ArrayList<>());
        template.getTemplateDays().add(newDay);
        return newDay;
    }

    /**
     * Appends a copy of every task on sourceDay onto targetDay. This never clears or
     * replaces targetDay's existing tasks - duplication is always additive.
     */
    private void duplicateTasksToDay(TemplateDay sourceDay, TemplateDay targetDay, Long currentUserId) {
        if (targetDay.getTemplateTasks() == null) {
            targetDay.setTemplateTasks(new ArrayList<>());
        }

        if (sourceDay.getTemplateTasks() != null) {
            // Snapshot first: sourceDay and targetDay can be the same list (self-duplicate),
            // so iterate a copy rather than the live list we're appending onto.
            int nextDisplayOrder = targetDay.getTemplateTasks().size();
            for (TemplateTask srcTask : new ArrayList<>(sourceDay.getTemplateTasks())) {
                TemplateTask targetTask = new TemplateTask();
                targetTask.setTitle(srcTask.getTitle());
                targetTask.setDescription(srcTask.getDescription());
                targetTask.setPriority(srcTask.getPriority());
                targetTask.setDisplayOrder(nextDisplayOrder++);
                targetTask.setTemplateDay(targetDay);
                targetTask.setCreatedBy(currentUserId);
                targetTask.setUpdatedBy(currentUserId);
                targetDay.getTemplateTasks().add(targetTask);
            }
        }
    }

    private RoleTemplateResponse convertToResponse(TaskBundle template) {
        RoleTemplateResponse response = new RoleTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setStatus(template.getStatus());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        response.setCreatedBy(template.getCreatedBy());
        response.setUpdatedBy(template.getUpdatedBy());

        if (template.getRole() != null) {
            response.setRoleId(template.getRole().getId());
            response.setRoleName(template.getRole().getName());
            response.setRoleDisplayName(template.getRole().getDisplayName());
        }

        if (template.getBranch() != null) {
            response.setBranchId(template.getBranch().getId());
            response.setBranchName(template.getBranch().getName());
        }

        if (template.getTemplateDays() != null) {
            List<RoleTemplateDayResponse> dayResponses = template.getTemplateDays().stream()
                    .map(day -> {
                        RoleTemplateDayResponse dayRes = new RoleTemplateDayResponse();
                        dayRes.setId(day.getId());
                        dayRes.setDayNumber(day.getDayNumber());
                        dayRes.setIsWeeklyCheckpoint(day.getIsWeeklyCheckpoint());
                        dayRes.setMonth(day.getMonth());
                        dayRes.setYear(day.getYear());

                        if (day.getTemplateTasks() != null) {
                            List<RoleTemplateTaskResponse> taskResponses = day.getTemplateTasks().stream()
                                    .map(this::convertTaskToResponse)
                                    .sorted(Comparator.comparing(RoleTemplateTaskResponse::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                                    .collect(Collectors.toList());
                            dayRes.setTasks(taskResponses);
                        } else {
                            dayRes.setTasks(new ArrayList<>());
                        }
                        return dayRes;
                    })
                    .sorted(Comparator.comparing(RoleTemplateDayResponse::getDayNumber)
                            .thenComparing(RoleTemplateDayResponse::getYear, Comparator.nullsFirst(Integer::compareTo))
                            .thenComparing(RoleTemplateDayResponse::getMonth, Comparator.nullsFirst(Integer::compareTo)))
                    .collect(Collectors.toList());
            response.setDays(dayResponses);
        } else {
            response.setDays(new ArrayList<>());
        }

        return response;
    }

    private RoleTemplateTaskResponse convertTaskToResponse(TemplateTask task) {
        RoleTemplateTaskResponse taskRes = new RoleTemplateTaskResponse();
        taskRes.setId(task.getId());
        taskRes.setTitle(task.getTitle());
        taskRes.setDescription(task.getDescription());
        taskRes.setPriority(task.getPriority());
        taskRes.setDisplayOrder(task.getDisplayOrder());
        return taskRes;
    }

    /**
     * Creates a single task on a day and persists it immediately, so the frontend never
     * holds task data only in local/draft state. The template must already exist - this
     * method deliberately never auto-creates one. A template can only come into existence
     * via {@link #createTemplate}, which requires a name (@NotBlank) and a role
     * (@NotNull), both enforced again by the NOT NULL columns on task_bundles. So the
     * caller (frontend) must save the template name first, capture the returned ID, and
     * only then start adding tasks; the day itself is created on first use if needed.
     */
    @Transactional
    public RoleTemplateTaskResponse addTaskToDay(Long templateId, Integer dayNumber, Integer month, Integer year, RoleTemplateTaskRequest request, Long currentUserId) {
        log.info("Adding task to template ID: {}, day: {}, month: {}, year: {}", templateId, dayNumber, month, year);
        TaskBundle template = taskBundleRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role template not found with ID: " + templateId
                                + ". Save the template name before adding tasks."));

        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new ResourceNotFoundException("Role template not found with ID: " + templateId);
        }

        if (template.getTemplateDays() == null) {
            template.setTemplateDays(new ArrayList<>());
        }

        TemplateDay day = findOrCreateDay(template, dayNumber, month, year, currentUserId);
        if (day.getTemplateTasks() == null) {
            day.setTemplateTasks(new ArrayList<>());
        }

        TemplateTask task = new TemplateTask();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        task.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : day.getTemplateTasks().size());
        task.setTemplateDay(day);
        task.setCreatedBy(currentUserId);
        task.setUpdatedBy(currentUserId);
        day.getTemplateTasks().add(task);

        taskBundleRepository.save(template);
        return convertTaskToResponse(task);
    }

    /**
     * Updates a single existing task in place and persists it immediately.
     */
    @Transactional
    public RoleTemplateTaskResponse updateTaskInDay(Long templateId, Integer dayNumber, Long taskId, RoleTemplateTaskRequest request, Long currentUserId) {
        log.info("Updating task ID: {} on template ID: {}, day: {}", taskId, templateId, dayNumber);
        TemplateTask task = findTaskInDay(templateId, taskId);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        if (request.getDisplayOrder() != null) {
            task.setDisplayOrder(request.getDisplayOrder());
        }
        task.setUpdatedBy(currentUserId);

        templateTaskRepository.save(task);
        return convertTaskToResponse(task);
    }

    /**
     * Deletes a single task immediately.
     */
    @Transactional
    public void deleteTaskFromDay(Long templateId, Integer dayNumber, Long taskId) {
        log.info("Deleting task ID: {} on template ID: {}, day: {}", taskId, templateId, dayNumber);
        TemplateTask task = findTaskInDay(templateId, taskId);
        // Entity-based delete(task) is a silent no-op here: the task is reachable via
        // TemplateDay.templateTasks (cascade=ALL, orphanRemoval=true), and once it's loaded
        // through that association JpaRepository#delete never emits a DELETE statement.
        // A direct bulk delete sidesteps that and reliably removes the row.
        templateTaskRepository.deleteByTaskId(task.getId());
    }

    /**
     * Resolves a task purely by its own (globally unique) id, searching every day under the
     * template rather than requiring the caller to know which day - recurring or
     * month-scoped - currently holds it. The {@code dayNumber}/month/year on the update and
     * delete routes are for URL context/logging only; once a template can have more than one
     * day sharing a dayNumber (one recurring, others scoped to a specific month), matching a
     * day first and then a task within it would be ambiguous, whereas the task id alone is
     * never ambiguous.
     */
    private TemplateTask findTaskInDay(Long templateId, Long taskId) {
        TaskBundle template = taskBundleRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Role template not found with ID: " + templateId));

        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new ResourceNotFoundException("Role template not found with ID: " + templateId);
        }

        return (template.getTemplateDays() == null ? List.<TemplateDay>of() : template.getTemplateDays()).stream()
                .flatMap(d -> (d.getTemplateTasks() == null ? List.<TemplateTask>of() : d.getTemplateTasks()).stream())
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId + " on template ID: " + templateId));
    }
}
