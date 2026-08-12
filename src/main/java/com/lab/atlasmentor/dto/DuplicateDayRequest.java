package com.lab.atlasmentor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class DuplicateDayRequest {
    @NotBlank(message = "Mode is required (SINGLE_DAY or RANGE)")
    private String mode;

    private Integer targetDayNumber;

    private Integer rangeLength;
}
