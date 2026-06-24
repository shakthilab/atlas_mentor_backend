package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "task_watchers",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"task_id", "user_id"},
                   name = "uk_task_watchers_task_user")
       },
       indexes = {
           @Index(name = "idx_task_watchers_task_id", columnList = "task_id"),
           @Index(name = "idx_task_watchers_user_id", columnList = "user_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskWatcher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"subtasks", "parentTask"})
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"reportingManager"})
    private User user;

    public TaskWatcher() {}

    public TaskWatcher(Task task, User user) {
        this.task = task;
        this.user = user;
    }
}
