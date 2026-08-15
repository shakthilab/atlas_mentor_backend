package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeeklyAccountabilityAnswerRequest {
    @NotNull(message = "Question ID is required")
    private Long questionId;

    private String answerText;
}
