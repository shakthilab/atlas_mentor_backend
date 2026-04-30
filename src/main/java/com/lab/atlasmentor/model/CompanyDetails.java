package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "company_details")
@Data
public class CompanyDetails extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User user;
    
    @Column(name = "company_name", length = 200)
    private String companyName;
    
    @Column(name = "contact_person", length = 150)
    private String contactPerson;
    
    @Column(name = "address", length = 500)
    private String address;
    
    @Column(name = "website", length = 500)
    private String website;
    
    @Column(name = "industry", length = 100)
    private String industry;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", referencedColumnName = "id", nullable = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User assignedTo;
    
    public CompanyDetails() {}
    
    public CompanyDetails(User user, String companyName, String contactPerson) {
        this.user = user;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
    }
    
    public CompanyDetails(User user, String companyName, String contactPerson, String website) {
        this.user = user;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.website = website;
    }
    
    public CompanyDetails(User user, String companyName, String contactPerson, String address, String website) {
        this.user = user;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.address = address;
        this.website = website;
    }
    
    public CompanyDetails(User user, String companyName, String contactPerson, String address, String website, String industry) {
        this.user = user;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.address = address;
        this.website = website;
        this.industry = industry;
    }
    
}
