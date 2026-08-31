package com.lab.atlasmentor.enums;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    COMPLETED,
    OVERDUE,
    REFLECT,
    /**
     * Terminal, employee-immutable state stamped once every reviewer has signed off
     * (day reaches ADMIN_VERIFIED - see DayApprovalService#approveDayLevel). DONE is
     * the employee's own "I'm finished" claim and stays mutable/re-flaggable; VERIFIED
     * means the whole review chain is done and there should be no more changes.
     */
    VERIFIED
}