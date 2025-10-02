package com.interexport.guarantees.entity.enums;

/**
 * Status of data import jobs for F12 Data Migration
 */
public enum ImportStatus {
    PENDING("Pending", "Import job queued for processing"),
    VALIDATING("Validating", "Validating file format and content"),
    IN_PROGRESS("In Progress", "Import processing in progress"),
    PAUSED("Paused", "Import paused at checkpoint"),
    COMPLETED("Completed", "Import completed successfully"),
    COMPLETED_WITH_ERRORS("Completed with Errors", "Import completed but some records failed"),
    FAILED("Failed", "Import failed due to critical error"),
    CANCELLED("Cancelled", "Import cancelled by user"),
    ROLLBACK_IN_PROGRESS("Rolling Back", "Rolling back imported data"),
    ROLLBACK_COMPLETED("Rollback Completed", "Data rollback completed successfully"),
    ROLLBACK_FAILED("Rollback Failed", "Data rollback failed");

    private final String displayName;
    private final String description;

    ImportStatus(String displayName, String description) {
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




