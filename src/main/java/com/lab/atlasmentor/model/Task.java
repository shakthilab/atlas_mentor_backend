package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.lab.atlasmentor.enums.TaskSource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "tasks",
       indexes = {
           @Index(name = "idx_tasks_assigned_to", columnList = "assigned_to"),
           @Index(name = "idx_tasks_assigned_by", columnList = "assigned_by"),
           @Index(name = "idx_tasks_created_by", columnList = "created_by"),
           @Index(name = "idx_tasks_branch_id", columnList = "branch_id"),
           @Index(name = "idx_tasks_status", columnList = "status"),
           @Index(name = "idx_tasks_priority", columnList = "priority"),
           @Index(name = "idx_tasks_due_date", columnList = "due_date"),
           @Index(name = "idx_tasks_reference", columnList = "reference_type, reference_id"),
           @Index(name = "idx_tasks_bundle_id", columnList = "task_bundle_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    @JsonIgnoreProperties({"tasks", "assignedTasks"})
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    @JsonIgnoreProperties({"tasks", "assignedTasks"})
    private User assignedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @JsonIgnoreProperties({"tasks", "assignedTasks"})
    private User createdByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnoreProperties({"users"})
    private Branch branch;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_bundle_id", nullable = true)
    @JsonIgnoreProperties({"tasks", "taskBundleTasks", "schedule"})
    private TaskBundle taskBundle;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private TaskSource sourceType = TaskSource.MANUAL;

    @Column(name = "execution_date")
    private LocalDate executionDate;

    @Column(name = "due_time")
    private LocalDateTime dueTime;

    public Task() {}

    public Task(String title, String description, User assignedTo, User assignedBy, User createdByUser, Priority priority, LocalDate dueDate, Branch branch) {
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.createdByUser = createdByUser;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.dueDate = dueDate;
        this.branch = branch;
        this.status = TaskStatus.PENDING;
        this.isDeleted = false;
        this.sourceType = TaskSource.MANUAL;
    }

    public Task(String title, String description, User assignedTo, User assignedBy, User createdByUser, Priority priority, LocalDate dueDate, Branch branch, TaskBundle taskBundle, TaskSource sourceType, LocalDate executionDate) {
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.createdByUser = createdByUser;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.dueDate = dueDate;
        this.branch = branch;
        this.status = TaskStatus.PENDING;
        this.isDeleted = false;
        this.taskBundle = taskBundle;
        this.sourceType = sourceType != null ? sourceType : TaskSource.MANUAL;
        this.executionDate = executionDate;
    }

    /**
     * Get branch ID for backward compatibility
     */
    public Long getBranchId() {
        return branch != null ? branch.getId() : null;
    }

    /**
     * Set branch by ID for backward compatibility
     */
    public void setBranchId(Long branchId) {
        if (branchId == null) {
            this.branch = null;
        } else {
            this.branch = new Branch();
            this.branch.setId(branchId);
        }
    }

    /**
     * Get task bundle ID for backward compatibility
     */
    public Long getTaskBundleId() {
        return taskBundle != null ? taskBundle.getId() : null;
    }

    /**
     * Set task bundle by ID for backward compatibility
     */
    public void setTaskBundleId(Long taskBundleId) {
        if (taskBundleId == null) {
            this.taskBundle = null;
        } else {
            this.taskBundle = new TaskBundle();
            this.taskBundle.setId(taskBundleId);
        }
    }
}
