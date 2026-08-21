package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
           @Index(name = "idx_tasks_bundle_id", columnList = "task_bundle_id"),
           @Index(name = "idx_tasks_task_list_id", columnList = "task_list_id"),
           @Index(name = "idx_tasks_parent_task_id", columnList = "parent_task_id"),
           @Index(name = "idx_tasks_start_date", columnList = "start_date")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", length = 20)
    private String displayId;

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

    // ---- ClickUp-style extensions (non-breaking additions) ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_list_id", nullable = true)
    @JsonIgnoreProperties({"tasks"})
    private TaskList taskList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_workspace_id", nullable = true)
    @JsonIgnoreProperties({"tasks"})
    private DayWorkspace dayWorkspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id", nullable = true)
    @JsonIgnoreProperties({"subtasks", "parentTask"})
    private Task parentTask;

    @OneToMany(mappedBy = "parentTask", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"parentTask"})
    private List<Task> subtasks;

    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * Which {@link TemplateTask} this task was instantiated from, when it came from a
     * Role Template day (null for manual/non-template tasks). Backs
     * uk_tasks_day_workspace_source_template_task (V10 migration): the DB-level guard
     * that keeps two racing instantiation triggers (cron, Publish, startup catch-up,
     * periodic safety-net) from ever creating the same template task twice for the same
     * employee's day.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_template_task_id", nullable = true)
    @JsonIgnoreProperties({"templateDay"})
    private TemplateTask sourceTemplateTask;

    // ---- Send-back / rework tracking (V12) - see DayApprovalService ----

    /** Which review stage (PARTNER_REVIEW/MANAGER_REVIEW/ADMIN_VERIFIED) owns this task's current reflect cycle, if any. */
    @Column(name = "reflect_stage", length = 20)
    private String reflectStage;

    /** FLAGGED (in REFLECT, employee must fix) or RESUBMITTED (fixed, awaiting reflectStage's re-review). Null outside a reflect cycle. */
    @Column(name = "reflect_state", length = 20)
    private String reflectState;

    @Column(name = "reflect_comment", columnDefinition = "TEXT")
    private String reflectComment;

    @Column(name = "reflect_flagged_at")
    private LocalDateTime reflectFlaggedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reflect_flagged_by")
    @JsonIgnoreProperties({"reportingManager"})
    private User reflectFlaggedBy;

    @Column(name = "reflect_resubmitted_at")
    private LocalDateTime reflectResubmittedAt;

    /** The status to restore on resubmit - whatever this task's status was at the moment it got flagged. */
    @Column(name = "reflect_previous_status", length = 20)
    private String reflectPreviousStatus;

    // ---- Per-task Day Approval Workflow step (V18) - see DayApprovalService#applyStepLabels ----

    /**
     * Friendly label for where this task sits right now: mirrors its day workspace's
     * approval_stage ("Not Submitted"/"Submitted"/"Branch Partner Review"/"Manager
     * Review"/"Verified"), or "Reflect"/"Awaiting &lt;stage&gt; Review" while it has its
     * own open send-back cycle (reflect_stage/reflect_state, V12). Null for tasks not
     * tied to a day workspace.
     */
    @Column(name = "current_step", length = 40)
    private String currentStep;

    /** What comes after {@link #currentStep} in the pipeline, or null when nothing follows (Verified, or awaiting re-review). */
    @Column(name = "next_step", length = 40)
    private String nextStep;

    // ---- Overdue rollover / completion tracking (V23) ----

    /**
     * The exact moment this task's status became DONE - set once, in
     * {@link com.lab.atlasmentor.service.TaskService#updateTaskStatus}, regardless of how many
     * days past {@link #dueDate} that happens. Independent of dueDate/dayWorkspace (neither of
     * which ever change once a task is created) - this is purely "when was it actually
     * finished," e.g. "originally due Aug 16, actually completed Aug 18." Null until then.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
