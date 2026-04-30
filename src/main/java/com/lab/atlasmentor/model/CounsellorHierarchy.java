package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "counsellor_hierarchy", 
       uniqueConstraints = @UniqueConstraint(columnNames = "junior_counsellor_id"))
@Data
public class CounsellorHierarchy extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senior_counsellor_id", referencedColumnName = "id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User seniorCounsellor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "junior_counsellor_id", referencedColumnName = "id", nullable = false, unique = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User juniorCounsellor;
    
    public CounsellorHierarchy() {}
    
    public CounsellorHierarchy(User seniorCounsellor, User juniorCounsellor) {
        this.seniorCounsellor = seniorCounsellor;
        this.juniorCounsellor = juniorCounsellor;
    }
    
}
