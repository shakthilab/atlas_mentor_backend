package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.enums.BundleStatus;
import com.lab.atlasmentor.enums.ScheduleType;
import com.lab.atlasmentor.exception.ResourceNotFoundException;
import com.lab.atlasmentor.exception.ValidationException;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing task bundles, including CRUD operations,
 * task management within bundles, and scheduling configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskBundleService {
    
    private final TaskBundleRepository taskBundleRepository;
    private final TaskBundleTaskRepository taskBundleTaskRepository;
    private final BundleScheduleRepository bundleScheduleRepository;
    private final BundleExecutionRepository bundleExecutionRepository;
    private final RoleRepository roleRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    
    /**
     * Create a new task bundle with tasks and schedule
     */
    public TaskBundleResponse createTaskBundle(CreateTaskBundleRequest request, Long currentUserId) {
        log.info("Creating task bundle: {} for role: {}", request.getName(), request.getRoleId());
        
        // Validate role exists
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + request.getRoleId()));
        
        // Check for duplicate bundle name within role
        Optional<TaskBundle> existingBundle = taskBundleRepository.findByNameAndRoleId(request.getName(), request.getRoleId());
        if (existingBundle.isPresent() && existingBundle.get().isActive()) {
            throw new ValidationException("Task bundle with name '" + request.getName() + "' already exists for this role");
        }
        
        // Create task bundle
        TaskBundle taskBundle = new TaskBundle();
        taskBundle.setName(request.getName());
        taskBundle.setDescription(request.getDescription());
        taskBundle.setRole(role);
        taskBundle.setStatus(request.getStatus());
        taskBundle.setCreatedBy(currentUserId);
        taskBundle.setUpdatedBy(currentUserId);
        
        taskBundle = taskBundleRepository.save(taskBundle);
        
        // Create schedule
        if (request.getSchedule() != null) {
            BundleSchedule schedule = createScheduleFromRequest(request.getSchedule(), taskBundle, currentUserId);
            taskBundle.setSchedule(schedule);
            
            // Calculate next execution date
            LocalDateTime nextExecution = calculateNextExecutionDate(schedule);
            taskBundle.setNextExecutionAt(nextExecution);
        }
        
        // Create task bundle tasks
        if (request.getTasks() != null && !request.getTasks().isEmpty()) {
            List<TaskBundleTask> taskBundleTasks = createTaskBundleTasksFromRequests(request.getTasks(), taskBundle, currentUserId);
            // Note: TaskBundle.tasks field is for actual Task entities, not TaskBundleTask
            // The TaskBundleTask entities are managed separately through their repository
        }
        
        taskBundle = taskBundleRepository.save(taskBundle);
        
        log.info("Task bundle created successfully with ID: {}", taskBundle.getId());
        return convertToResponse(taskBundle);
    }
    
    /**
     * Get task bundle by ID
     */
    @Transactional(readOnly = true)
    public TaskBundleResponse getTaskBundleById(Long id) {
        TaskBundle taskBundle = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task bundle not found with ID: " + id));
        
        return convertToResponse(taskBundle);
    }
    
    /**
     * Get all task bundles with optional filtering and summary stats
     */
    @Transactional(readOnly = true)
    public TaskBundlePageResponse getTaskBundles(Long roleId, BundleStatus status, ScheduleType scheduleType, String keyword, Pageable pageable) {
        String statusStr = status != null ? status.name() : null;
        String scheduleTypeStr = scheduleType != null ? scheduleType.name() : null;
        List<TaskBundle> filteredBundles = taskBundleRepository.findWithFilters(roleId, statusStr, scheduleTypeStr, keyword);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredBundles.size());
        List<TaskBundleListResponse> pageContent = filteredBundles.subList(start, end)
                .stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        long totalElements = filteredBundles.size();
        int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((double) totalElements / pageable.getPageSize()) : 1;

        long activeCount = taskBundleRepository.countByStatus(BundleStatus.ACTIVE);
        long inactiveCount = taskBundleRepository.countByStatus(BundleStatus.INACTIVE);
        long totalCount = taskBundleRepository.count();

        return new TaskBundlePageResponse(
                pageContent,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                totalElements,
                totalPages,
                totalCount,
                activeCount,
                inactiveCount
        );
    }
    
    /**
     * Update task bundle
     */
    public TaskBundleResponse updateTaskBundle(Long id, UpdateTaskBundleRequest request, Long currentUserId) {
        log.info("Updating task bundle: {}", id);
        
        TaskBundle taskBundle = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task bundle not found with ID: " + id));
        
        // Update bundle fields
        if (request.getName() != null) {
            // Check for duplicate name (excluding current bundle)
            Optional<TaskBundle> duplicateBundle = taskBundleRepository.findByNameAndRoleId(request.getName(), taskBundle.getRole().getId());
            if (duplicateBundle.isPresent() && !duplicateBundle.get().getId().equals(id) && duplicateBundle.get().isActive()) {
                throw new ValidationException("Task bundle with name '" + request.getName() + "' already exists for this role");
            }
            taskBundle.setName(request.getName());
        }
        
        if (request.getDescription() != null) {
            taskBundle.setDescription(request.getDescription());
        }
        
        if (request.getStatus() != null) {
            taskBundle.setStatus(request.getStatus());
        }
        
        taskBundle.setUpdatedBy(currentUserId);
        
        // Update schedule if provided
        if (request.getSchedule() != null) {
            BundleSchedule schedule = taskBundle.getSchedule();
            if (schedule == null) {
                schedule = new BundleSchedule();
                schedule.setTaskBundle(taskBundle);
            }
            updateScheduleFromRequest(request.getSchedule(), schedule, currentUserId);
            schedule = bundleScheduleRepository.save(schedule);
            taskBundle.setSchedule(schedule);
            
            // Recalculate next execution date
            LocalDateTime nextExecution = calculateNextExecutionDate(schedule);
            taskBundle.setNextExecutionAt(nextExecution);
        }
        
        // Update tasks if provided
        if (request.getTasks() != null) {
            updateTasksFromRequests(request.getTasks(), taskBundle, currentUserId);
        }
        
        taskBundle = taskBundleRepository.save(taskBundle);
        
        log.info("Task bundle updated successfully: {}", id);
        return convertToResponse(taskBundle);
    }
    
    /**
     * Delete task bundle (soft delete)
     */
    public void deleteTaskBundle(Long id, Long currentUserId) {
        log.info("Deleting task bundle: {}", id);
        
        TaskBundle taskBundle = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task bundle not found with ID: " + id));
        
        taskBundle.setStatus(BundleStatus.INACTIVE);
        taskBundle.setIsDeleted(true);
        taskBundle.setUpdatedBy(currentUserId);
        taskBundleRepository.save(taskBundle);
        
        log.info("Task bundle deleted successfully: {}", id);
    }
    
    /**
     * Activate task bundle
     */
    public TaskBundleResponse activateTaskBundle(Long id, Long currentUserId) {
        return updateBundleStatus(id, BundleStatus.ACTIVE, currentUserId);
    }
    
    /**
     * Deactivate task bundle
     */
    public TaskBundleResponse deactivateTaskBundle(Long id, Long currentUserId) {
        return updateBundleStatus(id, BundleStatus.INACTIVE, currentUserId);
    }
    
    /**
     * Get task bundles by role
     */
    @Transactional(readOnly = true)
    public List<TaskBundleListResponse> getTaskBundlesByRole(Long roleId) {
        List<TaskBundle> bundles = taskBundleRepository.findByRoleIdAndStatus(roleId, BundleStatus.ACTIVE);
        return bundles.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Add task to task bundle
     */
    public TaskBundleTaskResponse addTaskToBundle(Long bundleId, CreateTaskBundleTaskRequest request, Long currentUserId) {
        TaskBundle taskBundle = taskBundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("Task bundle not found with ID: " + bundleId));
        
        // Check for duplicate task title within bundle
        if (taskBundleTaskRepository.findByTaskBundleIdAndTitleAndIsDeletedFalse(bundleId, request.getTitle()).isPresent()) {
            throw new ValidationException("Task with title '" + request.getTitle() + "' already exists in this bundle");
        }
        
        // Get next task order
        Integer maxOrder = taskBundleTaskRepository.getMaxTaskOrderByTaskBundleId(bundleId);
        Integer nextOrder = (maxOrder != null ? maxOrder : 0) + 1;
        
        TaskBundleTask taskBundleTask = new TaskBundleTask();
        taskBundleTask.setTitle(request.getTitle());
        taskBundleTask.setDescription(request.getDescription());
        taskBundleTask.setTaskBundle(taskBundle);
        taskBundleTask.setPriority(request.getPriority());
        taskBundleTask.setTaskOrder(nextOrder);
        taskBundleTask.setDefaultDueDays(request.getDefaultDueDays());
        taskBundleTask.setIsActive(true);
        taskBundleTask.setIsDeleted(false);
        taskBundleTask.setCreatedBy(currentUserId);
        taskBundleTask.setUpdatedBy(currentUserId);
        
        taskBundleTask = taskBundleTaskRepository.save(taskBundleTask);
        
        return convertTaskBundleTaskToResponse(taskBundleTask);
    }
    
    /**
     * Update task in task bundle
     */
    public TaskBundleTaskResponse updateTaskInBundle(Long bundleId, Long taskId, UpdateTaskBundleTaskRequest request, Long currentUserId) {
        TaskBundleTask taskBundleTask = taskBundleTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task bundle task not found with ID: " + taskId));
        
        if (!taskBundleTask.getTaskBundle().getId().equals(bundleId)) {
            throw new ValidationException("Task does not belong to the specified bundle");
        }
        
        if (Boolean.TRUE.equals(taskBundleTask.getIsDeleted())) {
            throw new ResourceNotFoundException("Task bundle task not found with ID: " + taskId);
        }
        
        // Update task fields
        if (request.getTitle() != null) {
            // Check for duplicate title (excluding current task)
            if (taskBundleTaskRepository.findByTaskBundleIdAndTitleAndIsDeletedFalse(bundleId, request.getTitle())
                    .stream().anyMatch(t -> !t.getId().equals(taskId))) {
                throw new ValidationException("Task with title '" + request.getTitle() + "' already exists in this bundle");
            }
            taskBundleTask.setTitle(request.getTitle());
        }
        
        if (request.getDescription() != null) {
            taskBundleTask.setDescription(request.getDescription());
        }
        
        if (request.getPriority() != null) {
            taskBundleTask.setPriority(request.getPriority());
        }
        
        if (request.getTaskOrder() != null) {
            taskBundleTask.setTaskOrder(request.getTaskOrder());
        }
        
        if (request.getDefaultDueDays() != null) {
            taskBundleTask.setDefaultDueDays(request.getDefaultDueDays());
        }
        
        if (request.getIsActive() != null) {
            taskBundleTask.setIsActive(request.getIsActive());
        }
        
        taskBundleTask.setUpdatedBy(currentUserId);
        taskBundleTask = taskBundleTaskRepository.save(taskBundleTask);
        
        return convertTaskBundleTaskToResponse(taskBundleTask);
    }
    
    /**
     * Remove task from task bundle (soft delete)
     */
    public void removeTaskFromBundle(Long bundleId, Long taskId, Long currentUserId) {
        TaskBundleTask taskBundleTask = taskBundleTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task bundle task not found with ID: " + taskId));
        
        if (!taskBundleTask.getTaskBundle().getId().equals(bundleId)) {
            throw new ValidationException("Task does not belong to the specified bundle");
        }
        
        if (Boolean.TRUE.equals(taskBundleTask.getIsDeleted())) {
            throw new ResourceNotFoundException("Task bundle task not found with ID: " + taskId);
        }
        
        taskBundleTask.setIsDeleted(true);
        taskBundleTask.setUpdatedBy(currentUserId);
        taskBundleTaskRepository.save(taskBundleTask);
    }
    
    /**
     * Get tasks in task bundle along with bundle basic info
     */
    @Transactional(readOnly = true)
    public TaskBundleResponse getTasksInBundle(Long bundleId) {
        TaskBundle taskBundle = taskBundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("Task bundle not found with ID: " + bundleId));

        TaskBundleResponse response = new TaskBundleResponse();
        response.setId(taskBundle.getId());
        response.setName(taskBundle.getName());
        response.setDescription(taskBundle.getDescription());
        response.setStatus(taskBundle.getStatus());
        response.setLastExecutedAt(taskBundle.getLastExecutedAt());
        response.setNextExecutionAt(taskBundle.getNextExecutionAt());
        response.setCreatedAt(taskBundle.getCreatedAt());
        response.setUpdatedAt(taskBundle.getUpdatedAt());
        response.setCreatedBy(taskBundle.getCreatedBy());
        response.setUpdatedBy(taskBundle.getUpdatedBy());

        if (taskBundle.getRole() != null) {
            RoleResponse roleResponse = new RoleResponse();
            roleResponse.setId(taskBundle.getRole().getId());
            roleResponse.setName(taskBundle.getRole().getName());
            roleResponse.setDescription(taskBundle.getRole().getDescription());
            response.setRole(roleResponse);
        }

        if (taskBundle.getSchedule() != null) {
            response.setSchedule(convertScheduleToResponse(taskBundle.getSchedule()));
        }

        List<TaskBundleTask> taskBundleTasks = taskBundleTaskRepository.findByTaskBundleIdAndIsDeletedFalse(bundleId);
        response.setTasks(taskBundleTasks.stream()
                .map(this::convertTaskBundleTaskToResponse)
                .collect(Collectors.toList()));

        return response;
    }
    
    // Private helper methods
    
    private BundleSchedule createScheduleFromRequest(CreateBundleScheduleRequest request, TaskBundle taskBundle, Long currentUserId) {
        BundleSchedule schedule = new BundleSchedule();
        schedule.setTaskBundle(taskBundle);
        schedule.setScheduleType(request.getScheduleType());
        schedule.setExecutionTime(request.getExecutionTime());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setExecutionDay(request.getExecutionDay());
        schedule.setExecutionDayOfMonth(request.getExecutionDayOfMonth());
        schedule.setOneTimeExecutionDate(request.getOneTimeExecutionDate());
        schedule.setCreatedBy(currentUserId);
        schedule.setUpdatedBy(currentUserId);
        
        return bundleScheduleRepository.save(schedule);
    }
    
    private void updateScheduleFromRequest(UpdateBundleScheduleRequest request, BundleSchedule schedule, Long currentUserId) {
        if (request.getScheduleType() != null) {
            schedule.setScheduleType(request.getScheduleType());
        }
        if (request.getExecutionTime() != null) {
            schedule.setExecutionTime(request.getExecutionTime());
        }
        if (request.getStartDate() != null) {
            schedule.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            schedule.setEndDate(request.getEndDate());
        }
        if (request.getExecutionDay() != null) {
            schedule.setExecutionDay(request.getExecutionDay());
        }
        if (request.getExecutionDayOfMonth() != null) {
            schedule.setExecutionDayOfMonth(request.getExecutionDayOfMonth());
        }
        if (request.getOneTimeExecutionDate() != null) {
            schedule.setOneTimeExecutionDate(request.getOneTimeExecutionDate());
        }
        if (request.getIsActive() != null) {
            schedule.setIsActive(request.getIsActive());
        }
        
        schedule.setUpdatedBy(currentUserId);
    }
    
    private List<TaskBundleTask> createTaskBundleTasksFromRequests(List<CreateTaskBundleTaskRequest> requests, TaskBundle taskBundle, Long currentUserId) {
        List<TaskBundleTask> taskBundleTasks = new ArrayList<>();
        
        for (int i = 0; i < requests.size(); i++) {
            CreateTaskBundleTaskRequest request = requests.get(i);
            TaskBundleTask taskBundleTask = new TaskBundleTask();
            taskBundleTask.setTitle(request.getTitle());
            taskBundleTask.setDescription(request.getDescription());
            taskBundleTask.setTaskBundle(taskBundle);
            taskBundleTask.setPriority(request.getPriority());
            taskBundleTask.setTaskOrder(i + 1); // Set order based on position in list
            taskBundleTask.setDefaultDueDays(request.getDefaultDueDays());
            taskBundleTask.setIsActive(true);
            taskBundleTask.setIsDeleted(false);
            taskBundleTask.setCreatedBy(currentUserId);
            taskBundleTask.setUpdatedBy(currentUserId);
            taskBundleTasks.add(taskBundleTask);
        }
        
        // Save all task bundle tasks to the database
        return taskBundleTaskRepository.saveAll(taskBundleTasks);
    }
    
    private void updateTasksFromRequests(List<UpdateTaskBundleTaskRequest> requests, TaskBundle taskBundle, Long currentUserId) {
        requests.forEach(request -> {
            if (request.getId() != null) {
                taskBundleTaskRepository.findById(request.getId()).ifPresent(existing -> {
                    if (existing.getTaskBundle().getId().equals(taskBundle.getId())
                            && !Boolean.TRUE.equals(existing.getIsDeleted())) {
                        if (request.getTitle() != null) existing.setTitle(request.getTitle());
                        if (request.getDescription() != null) existing.setDescription(request.getDescription());
                        if (request.getPriority() != null) existing.setPriority(request.getPriority());
                        if (request.getTaskOrder() != null) existing.setTaskOrder(request.getTaskOrder());
                        if (request.getDefaultDueDays() != null) existing.setDefaultDueDays(request.getDefaultDueDays());
                        if (request.getIsActive() != null) existing.setIsActive(request.getIsActive());
                        existing.setUpdatedBy(currentUserId);
                        taskBundleTaskRepository.save(existing);
                    }
                });
            }
        });
    }
    
        
    private LocalDateTime calculateNextExecutionDate(BundleSchedule schedule) {
        LocalDate nextDate = schedule.calculateNextExecutionDate(null);
        if (nextDate != null && schedule.getExecutionTime() != null) {
            return LocalDateTime.of(nextDate, schedule.getExecutionTime());
        }
        return null;
    }
    
    private TaskBundleResponse updateBundleStatus(Long id, BundleStatus status, Long currentUserId) {
        TaskBundle taskBundle = taskBundleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task bundle not found with ID: " + id));
        
        taskBundle.setStatus(status);
        taskBundle.setUpdatedBy(currentUserId);
        
        if (status == BundleStatus.ACTIVE && taskBundle.getSchedule() != null) {
            LocalDateTime nextExecution = calculateNextExecutionDate(taskBundle.getSchedule());
            taskBundle.setNextExecutionAt(nextExecution);
        } else if (status == BundleStatus.INACTIVE) {
            taskBundle.setNextExecutionAt(null);
        }
        
        taskBundle = taskBundleRepository.save(taskBundle);
        return convertToResponse(taskBundle);
    }
    
    private TaskBundleResponse convertToResponse(TaskBundle taskBundle) {
        TaskBundleResponse response = new TaskBundleResponse();
        response.setId(taskBundle.getId());
        response.setName(taskBundle.getName());
        response.setDescription(taskBundle.getDescription());
        response.setStatus(taskBundle.getStatus());
        response.setLastExecutedAt(taskBundle.getLastExecutedAt());
        response.setNextExecutionAt(taskBundle.getNextExecutionAt());
        response.setCreatedAt(taskBundle.getCreatedAt());
        response.setUpdatedAt(taskBundle.getUpdatedAt());
        response.setCreatedBy(taskBundle.getCreatedBy());
        response.setUpdatedBy(taskBundle.getUpdatedBy());
        
        // Convert role
        if (taskBundle.getRole() != null) {
            RoleResponse roleResponse = new RoleResponse();
            roleResponse.setId(taskBundle.getRole().getId());
            roleResponse.setName(taskBundle.getRole().getName());
            roleResponse.setDescription(taskBundle.getRole().getDescription());
            response.setRole(roleResponse);
        }
        
        // Convert schedule
        if (taskBundle.getSchedule() != null) {
            response.setSchedule(convertScheduleToResponse(taskBundle.getSchedule()));
        }
        
        // Convert template task definitions from task_bundle_tasks (not generated Task instances)
        List<TaskBundleTask> templateTasks = taskBundleTaskRepository.findByTaskBundleIdAndIsDeletedFalse(taskBundle.getId());
        response.setTasks(templateTasks.stream()
                .map(this::convertTaskBundleTaskToResponse)
                .collect(Collectors.toList()));

        return response;
    }
    
    private TaskBundleListResponse convertToListResponse(TaskBundle taskBundle) {
        TaskBundleListResponse response = new TaskBundleListResponse();
        response.setId(taskBundle.getId());
        response.setName(taskBundle.getName());
        response.setDescription(taskBundle.getDescription());
        response.setStatus(taskBundle.getStatus());
        response.setLastExecutedAt(taskBundle.getLastExecutedAt());
        response.setNextExecutionAt(taskBundle.getNextExecutionAt());
        response.setCreatedAt(taskBundle.getCreatedAt());
        response.setCreatedBy(taskBundle.getCreatedBy());
        
        if (taskBundle.getRole() != null) {
            response.setRoleName(taskBundle.getRole().getName());
        }
        
        if (taskBundle.getSchedule() != null) {
            response.setScheduleType(taskBundle.getSchedule().getScheduleType().name());
        }
        
        if (taskBundle.getTasks() != null) {
            response.setActiveTaskCount((int) taskBundle.getTasks().stream()
                    .filter(task -> !Boolean.TRUE.equals(task.getIsDeleted()))
                    .count());
        }
        
        return response;
    }
    
    private BundleScheduleResponse convertScheduleToResponse(BundleSchedule schedule) {
        BundleScheduleResponse response = new BundleScheduleResponse();
        response.setId(schedule.getId());
        response.setScheduleType(schedule.getScheduleType());
        response.setExecutionTime(schedule.getExecutionTime());
        response.setStartDate(schedule.getStartDate());
        response.setEndDate(schedule.getEndDate());
        response.setExecutionDay(schedule.getExecutionDay() != null ? schedule.getExecutionDay().name() : null);
        response.setExecutionDayOfMonth(schedule.getExecutionDayOfMonth());
        response.setOneTimeExecutionDate(schedule.getOneTimeExecutionDate());
        response.setIsActive(schedule.getIsActive());
        response.setLastExecutionDate(schedule.getLastExecutionDate());
        response.setNextExecutionDate(schedule.getNextExecutionDate());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());
        response.setCreatedBy(schedule.getCreatedBy());
        response.setUpdatedBy(schedule.getUpdatedBy());
        
        return response;
    }
    
    private TaskBundleTaskResponse convertTaskToResponse(Task task) {
        TaskBundleTaskResponse response = new TaskBundleTaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority());
        response.setTaskOrder(0); // Default value since Task doesn't have taskOrder
        response.setDefaultDueDays(null); // Default value since Task doesn't have defaultDueDays
        response.setIsActive(Boolean.TRUE.equals(task.getIsDeleted()) ? Boolean.FALSE : Boolean.TRUE); // Inverse logic since Task uses isDeleted
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setCreatedBy(task.getCreatedBy());
        response.setUpdatedBy(task.getUpdatedBy());
        
        return response;
    }
    
    private TaskBundleTaskResponse convertTaskBundleTaskToResponse(TaskBundleTask taskBundleTask) {
        TaskBundleTaskResponse response = new TaskBundleTaskResponse();
        response.setId(taskBundleTask.getId());
        response.setTitle(taskBundleTask.getTitle());
        response.setDescription(taskBundleTask.getDescription());
        response.setPriority(taskBundleTask.getPriority());
        response.setTaskOrder(taskBundleTask.getTaskOrder());
        response.setDefaultDueDays(taskBundleTask.getDefaultDueDays());
        response.setIsActive(taskBundleTask.getIsActive());
        response.setCreatedAt(taskBundleTask.getCreatedAt());
        response.setUpdatedAt(taskBundleTask.getUpdatedAt());
        response.setCreatedBy(taskBundleTask.getCreatedBy());
        response.setUpdatedBy(taskBundleTask.getUpdatedBy());
        
        return response;
    }
}
