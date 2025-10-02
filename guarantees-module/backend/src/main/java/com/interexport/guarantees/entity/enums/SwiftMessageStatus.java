package com.interexport.guarantees.entity.enums;

/**
 * Status enumeration for SWIFT message processing
 * Tracks the lifecycle of SWIFT messages in the system
 */
public enum SwiftMessageStatus {
    /**
     * Message received but not yet parsed
     */
    RECEIVED("Received", "Message received from SWIFT network"),
    
    /**
     * Message is being parsed and validated
     */
    PROCESSING("Processing", "Message is being parsed and validated"),
    
    /**
     * Message parsed successfully and ready for business processing
     */
    PARSED("Parsed", "Message structure validated and parsed successfully"),
    
    /**
     * Business validation completed successfully
     */
    VALIDATED("Validated", "Business rules validation completed successfully"),
    
    /**
     * Message processing completed successfully
     */
    PROCESSED("Processed", "Message processed and guarantee/amendment created/updated"),
    
    /**
     * Response message generated and ready to send
     */
    RESPONDED("Responded", "Response message generated (MT768/769)"),
    
    /**
     * Message processing failed due to parse errors
     */
    PARSE_ERROR("Parse Error", "Failed to parse message structure or fields"),
    
    /**
     * Message failed business validation
     */
    VALIDATION_ERROR("Validation Error", "Failed business rules validation"),
    
    /**
     * Message processing failed due to system error
     */
    PROCESSING_ERROR("Processing Error", "System error during message processing"),
    
    /**
     * Message rejected due to business rules
     */
    REJECTED("Rejected", "Message rejected due to business policy violation"),
    
    /**
     * Message archived after successful processing
     */
    ARCHIVED("Archived", "Message archived for audit trail");

    private final String displayName;
    private final String description;

    SwiftMessageStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Check if status indicates an error state
     */
    public boolean isError() {
        return this == PARSE_ERROR || this == VALIDATION_ERROR || 
               this == PROCESSING_ERROR || this == REJECTED;
    }

    /**
     * Check if status indicates successful completion
     */
    public boolean isCompleted() {
        return this == PROCESSED || this == RESPONDED || this == ARCHIVED;
    }

    /**
     * Check if status indicates message is still being processed
     */
    public boolean isInProgress() {
        return this == RECEIVED || this == PROCESSING || this == PARSED || this == VALIDATED;
    }
}




