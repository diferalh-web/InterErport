package com.interexport.guarantees.entity.enums;

/**
 * Workflow steps for F11 Kafka Workflow Engine
 * Defines the various steps that can be executed in a post-approval workflow
 */
public enum WorkflowStep {
    // Post-approval steps
    COLLECT_COMMISSION("Collect Commission", "Collect commission for the guarantee"),
    SEND_NOTIFICATION("Send Notification", "Send notification to relevant parties"),
    UPDATE_ACCOUNTING("Update Accounting", "Update accounting records"),
    GENERATE_SWIFT("Generate SWIFT Message", "Generate and send SWIFT message"),
    UPDATE_EXTERNAL_SYSTEM("Update External System", "Update external systems"),
    ARCHIVE_DOCUMENTS("Archive Documents", "Archive related documents"),
    SEND_EMAIL_CONFIRMATION("Send Email Confirmation", "Send email confirmation to stakeholders"),
    LOG_AUDIT_TRAIL("Log Audit Trail", "Create audit trail entry");

    private final String displayName;
    private final String description;

    WorkflowStep(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}




