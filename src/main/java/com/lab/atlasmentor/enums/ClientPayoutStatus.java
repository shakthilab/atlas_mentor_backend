package com.lab.atlasmentor.enums;

import java.math.BigDecimal;

public enum ClientPayoutStatus {
    PENDING,           // Initial state, no amount assigned
    AMOUNT_ASSIGNED,   // Admin assigned amount, waiting for payment
    PARTIAL_PAID,      // Partial payment made
    PAID,              // Full payment completed
    DISPUTE,           // Dispute initiated
    ACCEPTED,          // Dispute accepted (no payout)
    REJECTED;          // Dispute rejected (back to previous status)
    
    public String getDisplayStatus(BigDecimal assignedAmount) {
        // Handle amount assignment display
        if (assignedAmount == null && this == PENDING) {
            return "amount is not assigned";
        }
        
        // Handle dispute status display
        if (this == ACCEPTED) {
            // Show as rejected when dispute is accepted by referral or company
            return "rejected";
        }
        
        return this.name();
    }
    
    public ClientPayoutStatus getEffectiveStatus(BigDecimal assignedAmount) {
        // Handle amount assignment display
        if (assignedAmount == null && this == PENDING) {
            return PENDING;
        }
        
        // Handle dispute status display
        if (this == ACCEPTED) {
            // Show as rejected when dispute is accepted by referral or company
            return REJECTED;
        }
        
        return this;
    }
}
