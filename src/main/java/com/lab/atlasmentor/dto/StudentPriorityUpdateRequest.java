package com.lab.atlasmentor.dto;

import com.lab.atlasmentor.enums.LeadPriority;
import com.lab.atlasmentor.enums.LeadPrioritySubCategory;

/**
 * Body for PUT /api/students/{id}/priority. Both fields are optional and independent — the
 * caller may send priority alone, prioritySubCategory alone, or both, to change either or both
 * in one call. A field left out (null) keeps the student's current value; whichever value ends
 * up in effect (submitted or existing) is validated together in
 * StudentService#updateStudentPriority using the same shared tier check
 * (LeadPrioritySubCategory#requireBelongsToTier) the create/edit path uses, so e.g. changing
 * priority to P1 while leaving a P2-only prioritySubCategory in place is rejected rather than
 * silently stored.
 */
public class StudentPriorityUpdateRequest {

    private LeadPriority priority;
    private LeadPrioritySubCategory prioritySubCategory;

    public StudentPriorityUpdateRequest() {}

    public LeadPriority getPriority() { return priority; }
    public void setPriority(LeadPriority priority) { this.priority = priority; }

    public LeadPrioritySubCategory getPrioritySubCategory() { return prioritySubCategory; }
    public void setPrioritySubCategory(LeadPrioritySubCategory prioritySubCategory) { this.prioritySubCategory = prioritySubCategory; }
}
