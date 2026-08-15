package com.lab.atlasmentor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class WeeklyAccountabilityResponseSubmitRequest {
    @NotNull(message = "checkpointDate is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkpointDate;

    @NotEmpty(message = "At least one answer is required")
    @Valid
    private List<WeeklyAccountabilityAnswerRequest> answers;
}
