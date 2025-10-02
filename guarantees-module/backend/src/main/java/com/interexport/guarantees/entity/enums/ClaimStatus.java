package com.interexport.guarantees.entity.enums;

/**
 * Status enumeration for Claims.
 */
public enum ClaimStatus {
    /**
     * Claim request received
     */
    REQUESTED("Requested"),
    
    /**
     * Under review
     */
    UNDER_REVIEW("Under Review"),
    
    /**
     * Documents missing - pending completion
     */
    PENDING_DOCUMENTS("Pending Documents"),
    
    /**
     * Approved for payment
     */
    APPROVED("Approved"),
    
    /**
     * Rejected
     */
    REJECTED("Rejected"),
    
    /**
     * Paid and settled
     */
    SETTLED("Settled");

    private final String displayName;

    ClaimStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if claim can be modified
     */
    public boolean isEditable() {
        return this == REQUESTED || this == UNDER_REVIEW || this == PENDING_DOCUMENTS;
    }

    /**
     * Check if claim can be paid
     */
    public boolean canPay() {
        return this == APPROVED;
    }
}
