package com.lab.atlasmentor.enums;

public enum StudentStatus {
        LEAD("Lead"),
        REGISTERED("Registered"),
        LOST("Lost"),
        PROSPECTIVE("Prospective"),
        CONTACTED("Contacted"),
        IN_PROGRESS("In Progress"),
        FOLLOWUP("Followup"),
        DOCUMENT_SUBMITTED("Document Submitted");

        private final String displayName;

        StudentStatus(String displayName) {
                this.displayName = displayName;
        }

        public String getDisplayName() {
                return displayName;
        }
}
