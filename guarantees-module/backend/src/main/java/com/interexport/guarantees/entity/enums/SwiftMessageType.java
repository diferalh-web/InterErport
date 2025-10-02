package com.interexport.guarantees.entity.enums;

/**
 * Enumeration of SWIFT message types for guarantee operations
 * Supporting F7 - SWIFT Integration requirements
 */
public enum SwiftMessageType {
    /**
     * MT760 - Received Guarantee
     * Used when a guarantee is issued by another bank
     */
    MT760("MT760", "Received Guarantee", "Incoming guarantee issued by correspondent bank"),
    
    /**
     * MT765 - Guarantee Amendment
     * Used for modifying existing guarantee terms
     */
    MT765("MT765", "Guarantee Amendment", "Amendment to existing guarantee terms"),
    
    /**
     * MT767 - Amendment Processing
     * Used to process and confirm guarantee amendments
     */
    MT767("MT767", "Amendment Processing", "Processing confirmation for guarantee amendments"),
    
    /**
     * MT768 - Acknowledgment of Received Guarantee
     * Sent to acknowledge receipt of MT760
     */
    MT768("MT768", "Acknowledgment", "Acknowledgment of received guarantee message"),
    
    /**
     * MT769 - Advice of Discrepancy
     * Used to report discrepancies in guarantee terms
     */
    MT769("MT769", "Discrepancy Advice", "Advice of discrepancy in guarantee terms"),
    
    /**
     * MT798 - Free Format Message
     * Used for general correspondence related to guarantees
     */
    MT798("MT798", "Free Format Message", "General correspondence message");

    private final String code;
    private final String displayName;
    private final String description;

    SwiftMessageType(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() {
        return code;
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
     * Find SwiftMessageType by code
     */
    public static SwiftMessageType fromCode(String code) {
        if (code == null) return null;
        
        for (SwiftMessageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}




