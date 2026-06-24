package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "task_assignments",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"task_id", "user_id"},
                   name = "uk_task_assignments_task_user")
       },
       indexes = {
           @Index(name = "idx_task_assignments_task_id", columnList = "task_id"),
           @Index(name = "idx_task_assignments_user_id", columnList = "user_id"),
           @Index(name = "idx_task_assignments_assigned_by", columnList = "assigned_by")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"assignments", "subtasks"})
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    @JsonIgnoreProperties({"reportingManager"})
    private User assignedBy;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (assignedAt == null) {
            assignedAt = java.time.LocalDateTime.now();
        }
    }

    public TaskAssignment() {}

    public TaskAssignment(Task task, User user, User assignedBy) {
        this.task = task;
        this.user = user;
        this.assignedBy = assignedBy;
        this.assignedAt = LocalDateTime.now();
    }
}
