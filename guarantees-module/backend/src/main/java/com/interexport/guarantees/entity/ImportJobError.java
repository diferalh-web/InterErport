package com.interexport.guarantees.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * ImportJobError entity for F12 Data Migration
 * Tracks individual errors during data import processing
 */
@Entity
@Table(name = "import_job_errors")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "importJob")
@ToString(exclude = "importJob")
public class ImportJobError extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;

    @Column(nullable = false)
    private Long recordNumber; // Line number or record position in file

    @Column(length = 100)
    private String errorType; // e.g., "VALIDATION_ERROR", "DUPLICATE_KEY", "FOREIGN_KEY_VIOLATION"

    @Column(length = 500)
    private String errorCode; // Specific error code for categorization

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Detailed error description

    @Column(columnDefinition = "TEXT")
    private String recordData; // The actual record data that failed

    @Column(columnDefinition = "TEXT")
    private String fieldErrors; // JSON with field-specific validation errors

    @Column(length = 200)
    private String affectedField; // The field that caused the error

    @Column(length = 500)
    private String suggestedFix; // Suggested way to fix the error

    private LocalDateTime errorTimestamp;

    private Boolean isResolved = false;
    private LocalDateTime resolvedAt;
    private String resolvedBy;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    // Severity levels
    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @Enumerated(EnumType.STRING)
    private ErrorSeverity severity = ErrorSeverity.MEDIUM;

    // Error categories for better organization
    public enum ErrorCategory {
        VALIDATION("Data validation failed"),
        DUPLICATE("Duplicate record found"),
        REFERENCE("Foreign key reference not found"),
        FORMAT("Data format error"),
        BUSINESS_RULE("Business rule violation"),
        SYSTEM("System or technical error"),
        PERMISSION("Permission or access error");

        private final String description;

        ErrorCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Enumerated(EnumType.STRING)
    private ErrorCategory category = ErrorCategory.VALIDATION;

    // Helper methods
    public boolean isCritical() {
        return severity == ErrorSeverity.CRITICAL;
    }

    public boolean isResolvable() {
        return suggestedFix != null && !suggestedFix.isEmpty();
    }

    public void markResolved(String resolvedBy, String resolutionNotes) {
        this.isResolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
    }
}




