package com.lab.atlasmentor.dto;

import lombok.Data;
import java.util.List;

@Data
public class EmployeeAssignmentRequest {
    
    private Long roleId;
    private Long managerId;
    private List<Long> userIds;
    
}
