package com.lab.atlasmentor.dto;

import lombok.Data;
import java.time.LocalDate;

/** One row in an approver's "needs my review" queue (Part E1). */
@Data
public class PendingApprovalResponse {
    private Long dayWorkspaceId;
    private Long employeeId;
    private String employeeName;
    private Long branchId;
    private String branchName;
    private LocalDate workDate;
    private Integer dayNumber;
    private Integer dailyCompletionPct;
    private String approvalStage;
}
