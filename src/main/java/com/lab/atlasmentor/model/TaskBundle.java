package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.BundleStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a task bundle that contains multiple tasks
 * and can be automatically scheduled for execution based on user roles.
 */
@Entity
@Table(name = "task_bundles", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"name", "role_id"}, 
                                name = "uk_task_bundles_name_role")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskBundle extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnoreProperties({"users", "taskBundles"})
    private Role role;
    
    @OneToOne(mappedBy = "taskBundle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"taskBundle"})
    private BundleSchedule schedule;
    
        
    @OneToMany(mappedBy = "taskBundle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"taskBundle"})
    private List<BundleExecution> executions;
    
    @OneToMany(mappedBy = "taskBundle", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"taskBundle"})
    private List<Task> tasks;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BundleStatus status = BundleStatus.ACTIVE;
    
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;
    
    @Column(name = "next_execution_at")
    private LocalDateTime nextExecutionAt;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // ---- ClickUp-style extensions (non-breaking additions) ----

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "icon", length = 50)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_branch_id", nullable = true)
    @JsonIgnoreProperties({"users"})
    private Branch branch;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "taskBundle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"taskBundle"})
    private List<TaskList> taskLists;

    @OneToMany(mappedBy = "roleTemplate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"roleTemplate"})
    private List<TemplateDay> templateDays;

    public TaskBundle() {}
    
    public TaskBundle(String name, String description, Role role, BundleStatus status) {
        this.name = name;
        this.description = description;
        this.role = role;
        this.status = status != null ? status : BundleStatus.ACTIVE;
    }
    
    /**
     * Get role ID for backward compatibility
     */
    public Long getRoleId() {
        return role != null ? role.getId() : null;
    }
    
    /**
     * Set role by ID for backward compatibility
     */
    public void setRoleId(Long roleId) {
        if (roleId == null) {
            this.role = null;
        } else {
            this.role = new Role();
            this.role.setId(roleId);
        }
    }
    
    /**
     * Check if bundle is currently active
     */
    public boolean isActive() {
        return BundleStatus.ACTIVE.equals(status);
    }
    
    /**
     * Check if bundle is ready for execution
     */
    public boolean isReadyForExecution() {
        return isActive() && nextExecutionAt != null && nextExecutionAt.isBefore(LocalDateTime.now());
    }
}
