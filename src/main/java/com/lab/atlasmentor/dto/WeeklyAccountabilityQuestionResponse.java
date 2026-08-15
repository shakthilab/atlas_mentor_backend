package com.lab.atlasmentor.dto;

import lombok.Data;

@Data
public class WeeklyAccountabilityQuestionResponse {
    private Long id;
    private String questionText;
    private Integer displayOrder;
}
