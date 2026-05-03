package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lab.atlasmentor.enums.StudentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_activities")
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentActivity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "id")
    private Student student;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private StudentStatus action;
    
    @Column(name = "old_value", length = 100)
    private String oldValue;
    
    @Column(name = "new_value", length = 100)
    private String newValue;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false, referencedColumnName = "id")
    private User performedBy;
    
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
    
    public StudentActivity() {}
    
    public StudentActivity(Student student, StudentStatus action, String oldValue, String newValue, String description, User performedBy) {
        this.student = student;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.description = description;
        this.performedBy = performedBy;
        this.performedAt = LocalDateTime.now();
    }
    
    public StudentActivity(Student student, StudentStatus action, String oldValue, String newValue, User performedBy) {
        this(student, action, oldValue, newValue, null, performedBy);
    }
}
