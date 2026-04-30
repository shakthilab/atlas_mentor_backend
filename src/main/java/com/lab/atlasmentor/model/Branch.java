package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.lab.atlasmentor.enums.UserStatus;

@Entity
@Table(name = "branches")
@Data
public class Branch extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 150)
    @NotBlank(message = "Branch name is required")
    @Size(min = 2, max = 150, message = "Branch name must be between 2 and 150 characters")
    private String name;
    
    @Column(name = "location", length = 255)
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = true)
    private User manager;
    
    public Branch() {}
    
    public Branch(String name, String location) {
        this.name = name;
        this.location = location;
    }
    
}
