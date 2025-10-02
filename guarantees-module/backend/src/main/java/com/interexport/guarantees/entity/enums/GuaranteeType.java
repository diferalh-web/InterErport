package com.interexport.guarantees.entity.enums;

/**
 * Types of guarantees based on SWIFT field 22A codes.
 */
public enum GuaranteeType {
    /**
     * Performance Guarantee
     */
    PERFORMANCE("PERF", "Performance Guarantee"),
    
    /**
     * Advance Payment Guarantee
     */
    ADVANCE_PAYMENT("ADPG", "Advance Payment Guarantee"),
    
    /**
     * Bid Bond / Tender Guarantee
     */
    BID_BOND("BIDB", "Bid Bond"),
    
    /**
     * Warranty Guarantee
     */
    WARRANTY("WARR", "Warranty Guarantee"),
    
    /**
     * Customs Guarantee
     */
    CUSTOMS("CUST", "Customs Guarantee"),
    
    /**
     * Payment Guarantee
     */
    PAYMENT("PAYG", "Payment Guarantee"),
    
    /**
     * Other type of guarantee
     */
    OTHER("OTHR", "Other");

    private final String swiftCode;
    private final String description;

    GuaranteeType(String swiftCode, String description) {
        this.swiftCode = swiftCode;
        this.description = description;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find guarantee type by SWIFT code
     */
    public static GuaranteeType fromSwiftCode(String code) {
        for (GuaranteeType type : values()) {
            if (type.swiftCode.equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
