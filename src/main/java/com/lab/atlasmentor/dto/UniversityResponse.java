package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UniversityResponse {
    private Long id;
    private String name;
    private CountryResponse country;
    private Long countryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UniversityResponse() {}

    public UniversityResponse(Long id, String name, CountryResponse country, Long countryId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.countryId = countryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
