package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** One entry in a day's approval trail (Part E3). */
@Data
public class DayApprovalResponse {
    private Long id;
    private String stage;
    private String action;
    private String comment;
    private Long approverId;
    private String approverName;
    private LocalDateTime actedAt;
}
