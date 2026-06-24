package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.DependencyType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "task_dependencies",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"task_id", "depends_on_task_id", "dependency_type"},
                   name = "uk_task_dependencies_unique")
       },
       indexes = {
           @Index(name = "idx_task_deps_task_id", columnList = "task_id"),
           @Index(name = "idx_task_deps_depends_on", columnList = "depends_on_task_id"),
           @Index(name = "idx_task_deps_type", columnList = "dependency_type")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskDependency extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"subtasks", "parentTask"})
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_task_id", nullable = false)
    @JsonIgnoreProperties({"subtasks", "parentTask"})
    private Task dependsOnTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 20)
    private DependencyType dependencyType;

    public TaskDependency() {}

    public TaskDependency(Task task, Task dependsOnTask, DependencyType dependencyType) {
        this.task = task;
        this.dependsOnTask = dependsOnTask;
        this.dependencyType = dependencyType;
    }
}
