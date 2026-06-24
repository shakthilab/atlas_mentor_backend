package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserReportingResponse {

    private Long id;
    private Long managerUserId;
    private String managerName;
    private String managerEmail;
    private Long employeeUserId;
    private String employeeName;
    private String employeeEmail;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
