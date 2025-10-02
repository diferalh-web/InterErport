package com.interexport.guarantees.dto;

import com.interexport.guarantees.entity.enums.AmendmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating amendments
 */
public class AmendmentCreateRequest {
    
    @NotNull(message = "Amendment type is required")
    private AmendmentType amendmentType;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
    
    @NotNull(message = "Changes JSON is required")
    private String changesJson;
    
    private Boolean requiresConsent;
    
    // Constructors
    public AmendmentCreateRequest() {}
    
    public AmendmentCreateRequest(AmendmentType amendmentType, String description, String reason, String changesJson, Boolean requiresConsent) {
        this.amendmentType = amendmentType;
        this.description = description;
        this.reason = reason;
        this.changesJson = changesJson;
        this.requiresConsent = requiresConsent;
    }
    
    // Getters and Setters
    public AmendmentType getAmendmentType() {
        return amendmentType;
    }
    
    public void setAmendmentType(AmendmentType amendmentType) {
        this.amendmentType = amendmentType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getChangesJson() {
        return changesJson;
    }
    
    public void setChangesJson(String changesJson) {
        this.changesJson = changesJson;
    }
    
    public Boolean getRequiresConsent() {
        return requiresConsent;
    }
    
    public void setRequiresConsent(Boolean requiresConsent) {
        this.requiresConsent = requiresConsent;
    }
}





