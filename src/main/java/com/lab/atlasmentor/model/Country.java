package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "countries")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Country extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(name = "code", nullable = false, unique = true, length = 5)
    private String code;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    public Country() {}
    
    public Country(String name, String code) {
        this.name = name;
        this.code = code;
    }
    
    public Country(String name, String code, User createdBy) {
        this.name = name;
        this.code = code;
        setCreatedBy(createdBy.getId());
    }
}
