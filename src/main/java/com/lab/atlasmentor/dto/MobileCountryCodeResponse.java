package com.lab.atlasmentor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileCountryCodeResponse {
    private Long id;
    private String countryName;
    private String countryCode;
    private String mobileCode;
    private String isoAlpha2;
    private String isoAlpha3;
    private Boolean isActive;
    
    public static MobileCountryCodeResponse fromEntity(com.lab.atlasmentor.model.MobileCountryCode entity) {
        MobileCountryCodeResponse response = new MobileCountryCodeResponse();
        response.setId(entity.getId());
        response.setCountryName(entity.getCountryName());
        response.setCountryCode(entity.getCountryCode());
        response.setMobileCode(entity.getMobileCode());
        response.setIsoAlpha2(entity.getIsoAlpha2());
        response.setIsoAlpha3(entity.getIsoAlpha3());
        response.setIsActive(entity.getIsActive());
        return response;
    }
}
