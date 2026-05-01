package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "universities")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class University extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false, referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Country country;
    
    @Column(name = "country_id", nullable = false, insertable = false, updatable = false)
    private Long countryId;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    public University() {}
    
    public University(String name, Country country) {
        this.name = name;
        this.country = country;
        this.countryId = country != null ? country.getId() : null;
    }
    
    public University(String name, Country country, User createdBy) {
        this.name = name;
        this.country = country;
        this.countryId = country != null ? country.getId() : null;
        setCreatedBy(createdBy);
    }
    
    public void setCountry(Country country) {
        this.country = country;
        this.countryId = country != null ? country.getId() : null;
    }
}
