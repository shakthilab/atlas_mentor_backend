package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.*;
import com.lab.atlasmentor.model.*;
import com.lab.atlasmentor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for continuous scheduling and execution of task bundles.
 * Runs periodically to check for bundles that need execution and triggers task generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BundleSchedulerService {
    
    private final TaskBundleRepository taskBundleRepository;
    private final BundleScheduleRepository bundleScheduleRepository;
    private final BundleExecutionRepository bundleExecutionRepository;
    private final TaskBundleTaskRepository taskBundleTaskRepository;
    private final TaskGenerationService taskGenerationService;
    
    /**
     * Main scheduler method that runs every minute to check for bundle executions
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void processScheduledBundles() {
        log.debug("Processing scheduled task bundles...");
        
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDate today = currentTime.toLocalDate();
            
            // Find bundles ready for execution
            List<TaskBundle> readyBundles = taskBundleRepository.findReadyForExecution(currentTime);
            
            if (readyBundles.isEmpty()) {
                log.debug("No bundles ready for execution at this time");
                return;
            }
            
            log.info("Found {} bundles ready for execution", readyBundles.size());
            
            for (TaskBundle bundle : readyBundles) {
                try {
                    executeBundle(bundle, currentTime);
                } catch (Exception e) {
                    log.error("Error executing bundle {}: {}", bundle.getId(), e.getMessage(), e);
                    recordBundleExecution(bundle, currentTime, "FAILED", 0, 0, e.getMessage(), null);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in bundle scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update next execution dates for all active bundles
     * Runs every hour to recalculate execution schedules
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void updateNextExecutionDates() {
        log.debug("Updating next execution dates for active bundles...");
        
        try {
            LocalDate currentDate = LocalDate.now();
            List<BundleSchedule> schedulesNeedingUpdate = bundleScheduleRepository.findNeedingNextExecutionUpdate(currentDate);
            
            for (BundleSchedule schedule : schedulesNeedingUpdate) {
                try {
                    updateBundleNextExecution(schedule);
                } catch (Exception e) {
                    log.error("Error updating next execution date for schedule {}: {}", schedule.getId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error updating next execution dates: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Execute a specific task bundle
     */
    public void executeBundle(TaskBundle bundle, LocalDateTime executionTime) {
        log.info("Executing bundle: {} ({})", bundle.getName(), bundle.getId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Check if bundle was already executed today
            if (bundleExecutionRepository.existsByTaskBundleIdAndExecutionDate(bundle.getId(), executionTime)) {
                log.info("Bundle {} was already executed today, skipping", bundle.getId());
                return;
            }
            
            // Get active tasks in the bundle
            List<TaskBundleTask> activeTasks = taskBundleTaskRepository.findActiveByTaskBundleId(bundle.getId());
            if (activeTasks.isEmpty()) {
                log.warn("Bundle {} has no active tasks, skipping execution", bundle.getId());
                recordBundleExecution(bundle, executionTime, "SUCCESS", 0, 0, null, System.currentTimeMillis() - startTime);
                return;
            }
            
            // Generate tasks for all users with the bundle's role
            TaskGenerationService.GenerationResult result = taskGenerationService.generateTasksForBundle(bundle, executionTime.toLocalDate());
            
            // Update bundle execution info
            bundle.setLastExecutedAt(executionTime);
            
            // Calculate next execution date
            if (bundle.getSchedule() != null) {
                LocalDate nextExecutionDate = bundle.getSchedule().calculateNextExecutionDate(executionTime.toLocalDate());
                if (nextExecutionDate != null && bundle.getSchedule().getExecutionTime() != null) {
                    bundle.setNextExecutionAt(LocalDateTime.of(nextExecutionDate, bundle.getSchedule().getExecutionTime()));
                }
                
                // Update schedule
                bundle.getSchedule().setLastExecutionDate(executionTime.toLocalDate());
                bundle.getSchedule().setNextExecutionDate(nextExecutionDate);
                bundleScheduleRepository.save(bundle.getSchedule());
            }
            
            taskBundleRepository.save(bundle);
            
            // Record successful execution
            recordBundleExecution(bundle, executionTime, "SUCCESS", result.getUsersCount(), result.getTasksGenerated(), null, System.currentTimeMillis() - startTime);
            
            log.info("Bundle {} executed successfully. Users: {}, Tasks: {}", 
                    bundle.getId(), result.getUsersCount(), result.getTasksGenerated());
            
        } catch (Exception e) {
            log.error("Error executing bundle {}: {}", bundle.getId(), e.getMessage(), e);
            recordBundleExecution(bundle, executionTime, "FAILED", 0, 0, e.getMessage(), System.currentTimeMillis() - startTime);
            throw e;
        }
    }
    
    /**
     * Manually trigger execution of a specific bundle
     */
    @Transactional
    public void triggerBundleExecution(Long bundleId) {
        TaskBundle bundle = taskBundleRepository.findById(bundleId)
                .orElseThrow(() -> new RuntimeException("Task bundle not found with ID: " + bundleId));
        
        if (!bundle.isActive()) {
            throw new RuntimeException("Task bundle is not active");
        }
        
        LocalDateTime executionTime = LocalDateTime.now();
        executeBundle(bundle, executionTime);
    }

    /**
     * Execute bundle now with detailed response.
     * Not @Transactional: each repo operation owns its own transaction so a
     * save failure cannot mark an outer transaction rollback-only.
     * Role and schedule are JOIN FETCH-ed to avoid LazyInitializationException
     * when they are accessed inside the REQUIRES_NEW task-generation transaction.
     */
    public BundleExecutionResult executeBundleNow(Long bundleId) {
        TaskBundle bundle = taskBundleRepository.findByIdWithDetails(bundleId)
                .orElseThrow(() -> new RuntimeException("Task bundle not found with ID: " + bundleId));

        LocalDateTime executionTime = LocalDateTime.now();

        if (!bundle.isActive()) {
            BundleExecutionResponse skipped = new BundleExecutionResponse();
            skipped.setExecutionDate(executionTime);
            skipped.setExecutionStatus("SKIPPED");
            skipped.setErrorMessage("Task bundle is not active");
            skipped.setUsersCount(0);
            skipped.setTasksGenerated(0);
            skipped.setExecutionDurationMs(0L);
            skipped.setNextExecutionDate(bundle.getNextExecutionAt());
            return new BundleExecutionResult(skipped, null);
        }

        try {
            // Execute the bundle
            executeBundle(bundle, executionTime);
            
            // Get the most recent execution record
            Optional<BundleExecution> executionOpt = bundleExecutionRepository.findFirstByTaskBundleIdOrderByExecutionDateDesc(bundleId);
            
            if (executionOpt.isEmpty()) {
                // Bundle was already executed today, create a response indicating this
                BundleExecutionResponse response = new BundleExecutionResponse();
                response.setExecutionDate(executionTime);
                response.setExecutionStatus("SKIPPED");
                response.setErrorMessage("Bundle was already executed today");
                response.setUsersCount(0);
                response.setTasksGenerated(0);
                response.setExecutionDurationMs(0L);
                response.setNextExecutionDate(bundle.getNextExecutionAt());
                
                // Set task bundle info
                TaskBundleResponse bundleResponse = new TaskBundleResponse();
                bundleResponse.setId(bundle.getId());
                bundleResponse.setName(bundle.getName());
                bundleResponse.setDescription(bundle.getDescription());
                bundleResponse.setStatus(bundle.getStatus());
                bundleResponse.setLastExecutedAt(bundle.getLastExecutedAt());
                bundleResponse.setNextExecutionAt(bundle.getNextExecutionAt());
                bundleResponse.setCreatedAt(bundle.getCreatedAt());
                bundleResponse.setUpdatedAt(bundle.getUpdatedAt());
                bundleResponse.setCreatedBy(bundle.getCreatedBy());
                bundleResponse.setUpdatedBy(bundle.getUpdatedBy());
                
                if (bundle.getRole() != null) {
                    RoleResponse roleResponse = new RoleResponse();
                    roleResponse.setId(bundle.getRole().getId());
                    roleResponse.setName(bundle.getRole().getName());
                    roleResponse.setDescription(bundle.getRole().getDescription());
                    bundleResponse.setRole(roleResponse);
                }
                
                response.setTaskBundle(bundleResponse);
                
                return new BundleExecutionResult(response, null);
            }
            
            BundleExecution execution = executionOpt.get();
            
            // Create response
            BundleExecutionResponse response = new BundleExecutionResponse();
            response.setId(execution.getId());
            response.setExecutionDate(execution.getExecutionDate());
            response.setUsersCount(execution.getUsersCount());
            response.setTasksGenerated(execution.getTasksGenerated());
            response.setExecutionStatus(execution.getExecutionStatus());
            response.setErrorMessage(execution.getErrorMessage());
            response.setExecutionDurationMs(execution.getExecutionDurationMs());
            response.setNextExecutionDate(bundle.getNextExecutionAt());
            response.setCreatedAt(execution.getCreatedAt());
            response.setUpdatedAt(execution.getUpdatedAt());
            response.setCreatedBy(execution.getCreatedBy());
            response.setUpdatedBy(execution.getUpdatedBy());
            
            // Set task bundle info
            TaskBundleResponse bundleResponse = new TaskBundleResponse();
            bundleResponse.setId(bundle.getId());
            bundleResponse.setName(bundle.getName());
            bundleResponse.setDescription(bundle.getDescription());
            bundleResponse.setStatus(bundle.getStatus());
            bundleResponse.setLastExecutedAt(bundle.getLastExecutedAt());
            bundleResponse.setNextExecutionAt(bundle.getNextExecutionAt());
            bundleResponse.setCreatedAt(bundle.getCreatedAt());
            bundleResponse.setUpdatedAt(bundle.getUpdatedAt());
            bundleResponse.setCreatedBy(bundle.getCreatedBy());
            bundleResponse.setUpdatedBy(bundle.getUpdatedBy());
            
            if (bundle.getRole() != null) {
                RoleResponse roleResponse = new RoleResponse();
                roleResponse.setId(bundle.getRole().getId());
                roleResponse.setName(bundle.getRole().getName());
                roleResponse.setDescription(bundle.getRole().getDescription());
                bundleResponse.setRole(roleResponse);
            }
            
            response.setTaskBundle(bundleResponse);
            
            return new BundleExecutionResult(response, null);
            
        } catch (Exception e) {
            BundleExecutionResponse errorResponse = new BundleExecutionResponse();
            errorResponse.setExecutionDate(executionTime);
            errorResponse.setExecutionStatus("FAILED");
            errorResponse.setErrorMessage(e.getMessage());
            errorResponse.setUsersCount(0);
            errorResponse.setTasksGenerated(0);
            errorResponse.setExecutionDurationMs(0L);
            
            return new BundleExecutionResult(errorResponse, e);
        }
    }
    
    /**
     * Update next execution date for a bundle schedule
     */
    private void updateBundleNextExecution(BundleSchedule schedule) {
        if (!schedule.isCurrentlyActive()) {
            return;
        }
        
        LocalDate nextExecutionDate = schedule.calculateNextExecutionDate(null);
        schedule.setNextExecutionDate(nextExecutionDate);
        
        // Update task bundle's next execution time
        TaskBundle bundle = schedule.getTaskBundle();
        if (bundle != null && nextExecutionDate != null && schedule.getExecutionTime() != null) {
            bundle.setNextExecutionAt(LocalDateTime.of(nextExecutionDate, schedule.getExecutionTime()));
            taskBundleRepository.save(bundle);
        }
        
        bundleScheduleRepository.save(schedule);
    }
    
    /**
     * Record bundle execution in the execution history
     */
    private void recordBundleExecution(TaskBundle bundle, LocalDateTime executionTime, String status, 
                                   int usersCount, int tasksGenerated, String errorMessage, Long durationMs) {
        BundleExecution execution = new BundleExecution();
        execution.setTaskBundle(bundle);
        execution.setExecutionDate(executionTime);
        execution.setExecutionStatus(status);
        execution.setUsersCount(usersCount);
        execution.setTasksGenerated(tasksGenerated);
        execution.setErrorMessage(errorMessage);
        execution.setExecutionDurationMs(durationMs);
        execution.setCreatedBy(1L); // System user ID
        execution.setUpdatedBy(1L);
        
        bundleExecutionRepository.save(execution);
        
        log.debug("Recorded bundle execution: bundle={}, status={}, users={}, tasks={}, duration={}ms", 
                bundle.getId(), status, usersCount, tasksGenerated, durationMs);
    }
    
    /**
     * Get execution statistics for a bundle
     */
    @Transactional(readOnly = true)
    public BundleExecutionStats getBundleExecutionStats(Long bundleId) {
        List<Object[]> stats = bundleExecutionRepository.getExecutionStatsByTaskBundleId(bundleId);
        
        BundleExecutionStats bundleStats = new BundleExecutionStats();
        bundleStats.setBundleId(bundleId);
        
        for (Object[] stat : stats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];
            Long totalTasks = (Long) stat[2];
            Double avgDuration = (Double) stat[3];
            
            switch (status) {
                case "SUCCESS":
                    bundleStats.setSuccessfulExecutions(count.intValue());
                    bundleStats.setTotalTasksGenerated(totalTasks.intValue());
                    bundleStats.setAverageExecutionDuration(avgDuration != null ? avgDuration.longValue() : 0L);
                    break;
                case "FAILED":
                    bundleStats.setFailedExecutions(count.intValue());
                    break;
                case "PARTIAL":
                    bundleStats.setPartialExecutions(count.intValue());
                    break;
            }
        }
        
        bundleStats.setTotalExecutions(bundleStats.getSuccessfulExecutions() + 
                                    bundleStats.getFailedExecutions() + 
                                    bundleStats.getPartialExecutions());
        
        return bundleStats;
    }
    
    /**
     * Check if any bundles are scheduled for execution in the next hour
     */
    @Transactional(readOnly = true)
    public List<TaskBundle> getBundlesScheduledNextHour() {
        LocalDateTime oneHourFromNow = LocalDateTime.now().plusHours(1);
        return taskBundleRepository.findReadyForExecution(oneHourFromNow);
    }
    
    /**
     * DTO for bundle execution statistics
     */
    public static class BundleExecutionStats {
        private Long bundleId;
        private Integer totalExecutions;
        private Integer successfulExecutions;
        private Integer failedExecutions;
        private Integer partialExecutions;
        private Integer totalTasksGenerated;
        private Long averageExecutionDuration;
        
        // Getters and setters
        public Long getBundleId() { return bundleId; }
        public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
        
        public Integer getTotalExecutions() { return totalExecutions; }
        public void setTotalExecutions(Integer totalExecutions) { this.totalExecutions = totalExecutions; }
        
        public Integer getSuccessfulExecutions() { return successfulExecutions; }
        public void setSuccessfulExecutions(Integer successfulExecutions) { this.successfulExecutions = successfulExecutions; }
        
        public Integer getFailedExecutions() { return failedExecutions; }
        public void setFailedExecutions(Integer failedExecutions) { this.failedExecutions = failedExecutions; }
        
        public Integer getPartialExecutions() { return partialExecutions; }
        public void setPartialExecutions(Integer partialExecutions) { this.partialExecutions = partialExecutions; }
        
        public Integer getTotalTasksGenerated() { return totalTasksGenerated; }
        public void setTotalTasksGenerated(Integer totalTasksGenerated) { this.totalTasksGenerated = totalTasksGenerated; }
        
        public Long getAverageExecutionDuration() { return averageExecutionDuration; }
        public void setAverageExecutionDuration(Long averageExecutionDuration) { this.averageExecutionDuration = averageExecutionDuration; }
        
        public Double getSuccessRate() {
            if (totalExecutions == null || totalExecutions == 0) return 0.0;
            return (double) successfulExecutions / totalExecutions * 100;
        }
    }
    
    /**
     * Result wrapper for bundle execution with response and optional error
     */
    public static class BundleExecutionResult {
        private final BundleExecutionResponse response;
        private final Exception error;
        
        public BundleExecutionResult(BundleExecutionResponse response, Exception error) {
            this.response = response;
            this.error = error;
        }
        
        public BundleExecutionResponse getResponse() {
            return response;
        }
        
        public Exception getError() {
            return error;
        }
        
        public boolean isSuccess() {
            return error == null && response != null && "SUCCESS".equals(response.getExecutionStatus());
        }
        
        public boolean isFailure() {
            return error != null || "FAILED".equals(response.getExecutionStatus());
        }
    }
}
