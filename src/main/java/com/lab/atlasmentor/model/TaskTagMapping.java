package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "task_tag_mapping",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"task_id", "tag_id"},
                   name = "uk_task_tag_mapping_task_tag")
       },
       indexes = {
           @Index(name = "idx_task_tag_mapping_task_id", columnList = "task_id"),
           @Index(name = "idx_task_tag_mapping_tag_id", columnList = "tag_id")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskTagMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"subtasks", "parentTask"})
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private TaskTag tag;

    public TaskTagMapping() {}

    public TaskTagMapping(Task task, TaskTag tag) {
        this.task = task;
        this.tag = tag;
    }
}
