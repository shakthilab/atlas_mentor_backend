package com.lab.atlasmentor.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "mobile_country_codes")
@Data
public class MobileCountryCode extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName;
    
    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;
    
    @Column(name = "mobile_code", nullable = false, length = 10)
    private String mobileCode;
    
    @Column(name = "iso_alpha_2", nullable = false, length = 2, unique = true)
    private String isoAlpha2;
    
    @Column(name = "iso_alpha_3", nullable = false, length = 3, unique = true)
    private String isoAlpha3;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "flag_url", length = 254)
    private String flagUrl;
    
    @Column(name = "mobile_number_length")
    private Integer mobileNumberLength;
    
    public MobileCountryCode() {}
    
    public MobileCountryCode(String countryName, String countryCode, String mobileCode, 
                           String isoAlpha2, String isoAlpha3) {
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.mobileCode = mobileCode;
        this.isoAlpha2 = isoAlpha2;
        this.isoAlpha3 = isoAlpha3;
        this.isActive = true;
    }
}
