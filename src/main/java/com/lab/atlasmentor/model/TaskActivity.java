package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.atlasmentor.enums.TaskAction;

@Entity
@Table(name = "task_activity",
       indexes = {
           @Index(name = "idx_task_activity_task_id", columnList = "task_id"),
           @Index(name = "idx_task_activity_done_by", columnList = "done_by")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"activities", "taskActivities"})
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private TaskAction action;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    /** Free-text detail attached directly to this activity entry - e.g. a reviewer's send-back comment. */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "done_by", nullable = false)
    @JsonIgnoreProperties({"activities", "taskActivities"})
    private User doneBy;

    public TaskActivity() {}

    public TaskActivity(Task task, TaskAction action, String oldValue, String newValue, User doneBy) {
        this.task = task;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.doneBy = doneBy;
    }

    public TaskActivity(Task task, TaskAction action, String oldValue, String newValue, User doneBy, String comment) {
        this(task, action, oldValue, newValue, doneBy);
        this.comment = comment;
    }
}
