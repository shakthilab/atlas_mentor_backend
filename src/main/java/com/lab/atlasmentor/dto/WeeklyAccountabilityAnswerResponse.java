package com.lab.atlasmentor.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class WeeklyAccountabilityAnswerResponse {
    private Long id;
    private Integer weekNumber;
    private Long questionId;
    private String questionText;
    private LocalDate checkpointDate;
    private String answerText;
    private LocalDateTime answeredAt;
}
