package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.lab.atlasmentor.enums.ReferralType;

@Entity
@Table(name = "referral_details",
       indexes = {
           @Index(name = "idx_referral_details_type", columnList = "referral_type")
       })
@Data
@EqualsAndHashCode(callSuper = false)
public class ReferralDetails extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "referral_type", nullable = false)
    private ReferralType referralType;
    
    public ReferralDetails() {}
    
    public ReferralDetails(User user, ReferralType referralType) {
        this.user = user;
        this.referralType = referralType;
    }
    
    // Backward compatibility method
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
    public void setUserId(Long userId) {
        if (userId != null) {
            this.user = new User();
            this.user.setId(userId);
        }
    }
    
}
