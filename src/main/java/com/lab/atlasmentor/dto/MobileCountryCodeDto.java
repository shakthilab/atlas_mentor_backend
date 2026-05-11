package com.lab.atlasmentor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileCountryCodeDto {
    
    private Long id;
    private String countryName;
    private String countryCode;
    private String mobileCode;
    private String isoAlpha2;
    private String isoAlpha3;
    private Boolean isActive;
    private String flagUrl;
    private Integer mobileNumberLength;
}
