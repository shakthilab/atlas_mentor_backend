package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "roles")
@Data
@EqualsAndHashCode(callSuper = false)
public class Role extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "is_employee", nullable = false)
    private Boolean isEmployee = false;

    @Column(name = "code", length = 10)
    private String code;

    public Role() {}
    
    public Role(String name) {
        this.name = name;
    }
    
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public Role(String name, String description, Boolean isEmployee) {
        this.name = name;
        this.description = description;
        this.isEmployee = isEmployee;
    }
}
