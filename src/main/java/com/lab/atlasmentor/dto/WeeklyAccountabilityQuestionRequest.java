package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WeeklyAccountabilityQuestionRequest {
    /** Present -> update that existing question; absent -> create a new one. */
    private Long id;

    @NotBlank(message = "Question text is required")
    private String questionText;

    private Integer displayOrder = 0;
}
