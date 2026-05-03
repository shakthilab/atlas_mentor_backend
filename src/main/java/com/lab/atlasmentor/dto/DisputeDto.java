package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.DisputeStatus;
import com.lab.atlasmentor.enums.DisputePriority;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DisputeDto {
    
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long relatedApprovalId;
    private Long raisedBy;
    private String raisedByName;
    private Long resolvedBy;
    private String resolvedByName;
    private DisputeStatus status;
    private DisputePriority priority;
    private String disputeReason;
    private String resolutionNotes;
    private LocalDateTime raisedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime resolutionDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public DisputeDto() {}
    
    public DisputeDto(Long id, Long studentId, String studentName, String studentEmail,
                     Long relatedApprovalId, Long raisedBy, String raisedByName,
                     Long resolvedBy, String resolvedByName, DisputeStatus status,
                     DisputePriority priority, String disputeReason, String resolutionNotes,
                     LocalDateTime raisedAt, LocalDateTime resolvedAt, LocalDateTime resolutionDeadline,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.relatedApprovalId = relatedApprovalId;
        this.raisedBy = raisedBy;
        this.raisedByName = raisedByName;
        this.resolvedBy = resolvedBy;
        this.resolvedByName = resolvedByName;
        this.status = status;
        this.priority = priority;
        this.disputeReason = disputeReason;
        this.resolutionNotes = resolutionNotes;
        this.raisedAt = raisedAt;
        this.resolvedAt = resolvedAt;
        this.resolutionDeadline = resolutionDeadline;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
