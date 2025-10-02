package com.interexport.guarantees.entity.enums;

/**
 * Status enumeration for Guarantee contracts.
 * Represents the lifecycle states of a guarantee.
 */
public enum GuaranteeStatus {
    /**
     * Initial draft state - can be edited
     */
    DRAFT("Draft"),
    
    /**
     * Submitted for approval
     */
    SUBMITTED("Submitted"),
    
    /**
     * Approved and active
     */
    APPROVED("Approved"),
    
    /**
     * Rejected during approval process
     */
    REJECTED("Rejected"),
    
    /**
     * Cancelled before activation
     */
    CANCELLED("Cancelled"),
    
    /**
     * Expired guarantee
     */
    EXPIRED("Expired"),
    
    /**
     * Claimed and settled
     */
    SETTLED("Settled"),
    
    /**
     * Received from SWIFT message
     */
    RECEIVED("Received");

    private final String displayName;

    GuaranteeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if the guarantee can be edited in this status
     */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }

    /**
     * Check if the guarantee can be cancelled in this status
     */
    public boolean isCancellable() {
        return this == DRAFT || this == SUBMITTED || this == APPROVED;
    }

    /**
     * Check if amendments can be created in this status
     */
    public boolean canAmend() {
        return this == APPROVED;
    }

    /**
     * Check if claims can be created in this status
     */
    public boolean canClaim() {
        return this == APPROVED;
    }
}
