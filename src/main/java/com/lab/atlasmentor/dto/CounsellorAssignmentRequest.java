package com.lab.atlasmentor.dto;

import lombok.Data;
import java.util.List;

@Data
public class CounsellorAssignmentRequest {
    
    private Long seniorCounsellorId;
    private List<Long> juniorCounsellorIds;
    
}
