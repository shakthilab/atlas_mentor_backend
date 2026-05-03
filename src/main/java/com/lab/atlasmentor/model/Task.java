package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.TaskStatus;
import com.lab.atlasmentor.enums.Priority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "tasks")
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

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnoreProperties({"users"})
    private Branch branch;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

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
}
