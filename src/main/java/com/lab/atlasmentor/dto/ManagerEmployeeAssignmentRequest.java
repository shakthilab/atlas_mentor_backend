package com.lab.atlasmentor.dto;

import lombok.Data;

@Data
public class ManagerEmployeeAssignmentRequest {
    
    private Long managerId;
    private Long employeeId;
    
}
