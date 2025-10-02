package com.interexport.guarantees.entity.enums;

/**
 * Types of amendments based on business requirements and SWIFT message types.
 */
public enum AmendmentType {
    /**
     * Increase guarantee amount
     */
    AMOUNT_INCREASE("GITRAM", "Amount Increase"),
    
    /**
     * Decrease guarantee amount
     */
    AMOUNT_DECREASE("GITAME", "Amount Decrease"),
    
    /**
     * Extend validity period
     */
    EXTEND_VALIDITY("GITRAM", "Extend Validity"),
    
    /**
     * Reduce validity period
     */
    REDUCE_VALIDITY("GITAME", "Reduce Validity"),
    
    /**
     * Change beneficiary details
     */
    CHANGE_BENEFICIARY("GITRAM", "Change Beneficiary"),
    
    /**
     * Change currency
     */
    CHANGE_CURRENCY("GITRAM", "Change Currency"),
    
    /**
     * Other amendment types
     */
    OTHER("GITRAM", "Other Amendment");

    private final String swiftCode;
    private final String description;

    AmendmentType(String swiftCode, String description) {
        this.swiftCode = swiftCode;
        this.description = description;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public String getDescription() {
        return description;
    }
}
