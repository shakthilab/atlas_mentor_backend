package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "task_tags",
       indexes = {
           @Index(name = "idx_task_tags_name", columnList = "name")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "color", length = 20)
    private String color;

    public TaskTag() {}

    public TaskTag(String name, String color) {
        this.name = name;
        this.color = color;
    }
}
