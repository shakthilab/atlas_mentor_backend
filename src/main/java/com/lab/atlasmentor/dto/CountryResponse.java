package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CountryResponse {
    private Long id;
    private String name;
    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CountryResponse() {}

    public CountryResponse(Long id, String name, String code, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
