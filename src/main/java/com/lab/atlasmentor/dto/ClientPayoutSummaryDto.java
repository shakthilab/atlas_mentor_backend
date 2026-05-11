package com.lab.atlasmentor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ClientPayoutSummaryDto {
    
    // Count-based statistics
    private Long totalAssigned;      // Count of payouts with assigned amount
    private Long totalPaid;          // Count of fully paid payouts
    private Long totalPending;       // Count of pending payouts (no amount assigned)
    private Long pendingApprovals;   // Count of payouts pending approval
    private Long disputes;           // Count of disputed payouts
    private Long rejected;           // Count of rejected payouts
    
    // Amount-based statistics
    private BigDecimal totalAssignedAmount;  // Sum of all assigned amounts
    private BigDecimal totalPaidAmount;      // Sum of all paid amounts
    private BigDecimal totalPendingAmount;   // Sum of pending amounts (assigned but not paid)
    private BigDecimal totalDisputedAmount;  // Sum of disputed amounts
    private BigDecimal totalRejectedAmount;  // Sum of rejected amounts
    
    // Additional useful statistics
    private Long partialPayments;   // Count of partially paid payouts
    private BigDecimal totalPartialAmount; // Sum of partially paid amounts
    
    public ClientPayoutSummaryDto(Long totalAssigned, Long totalPaid, Long totalPending, 
                                 Long pendingApprovals, Long disputes) {
        this.totalAssigned = totalAssigned;
        this.totalPaid = totalPaid;
        this.totalPending = totalPending;
        this.pendingApprovals = pendingApprovals;
        this.disputes = disputes;
        this.rejected = 0L;
        this.totalAssignedAmount = BigDecimal.ZERO;
        this.totalPaidAmount = BigDecimal.ZERO;
        this.totalPendingAmount = BigDecimal.ZERO;
        this.totalDisputedAmount = BigDecimal.ZERO;
        this.totalRejectedAmount = BigDecimal.ZERO;
        this.partialPayments = 0L;
        this.totalPartialAmount = BigDecimal.ZERO;
    }
    
    public ClientPayoutSummaryDto(Long totalAssigned, Long totalPaid, Long totalPending, 
                                 Long pendingApprovals, Long disputes,
                                 BigDecimal totalAssignedAmount, BigDecimal totalPaidAmount, 
                                 BigDecimal totalPendingAmount, BigDecimal totalDisputedAmount,
                                 Long partialPayments, BigDecimal totalPartialAmount) {
        this.totalAssigned = totalAssigned;
        this.totalPaid = totalPaid;
        this.totalPending = totalPending;
        this.pendingApprovals = pendingApprovals;
        this.disputes = disputes;
        this.rejected = 0L;
        this.totalAssignedAmount = totalAssignedAmount;
        this.totalPaidAmount = totalPaidAmount;
        this.totalPendingAmount = totalPendingAmount;
        this.totalDisputedAmount = totalDisputedAmount;
        this.totalRejectedAmount = BigDecimal.ZERO;
        this.partialPayments = partialPayments;
        this.totalPartialAmount = totalPartialAmount;
    }
}
