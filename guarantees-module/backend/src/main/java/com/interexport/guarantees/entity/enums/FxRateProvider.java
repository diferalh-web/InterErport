package com.interexport.guarantees.entity.enums;

/**
 * Foreign exchange rate providers.
 */
public enum FxRateProvider {
    /**
     * Manual entry by administrators
     */
    MANUAL("Manual Entry"),
    
    /**
     * European Central Bank API
     */
    ECB("European Central Bank"),
    
    /**
     * Bloomberg API
     */
    BLOOMBERG("Bloomberg");

    private final String description;

    FxRateProvider(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if provider requires API integration
     */
    public boolean requiresApi() {
        return this != MANUAL;
    }
}
