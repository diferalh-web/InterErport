package com.interexport.guarantees.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new claim to avoid validation issues with the entity
 */
public class ClaimCreateRequest {

    @NotBlank(message = "Claim reference is required")
    @Size(max = 35, message = "Claim reference cannot exceed 35 characters")
    private String claimReference;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(max = 3, message = "Currency code cannot exceed 3 characters")
    private String currency;

    @NotNull(message = "Claim date is required")
    private LocalDate claimDate;

    @NotBlank(message = "Claim reason is required")
    @Size(max = 2000, message = "Claim reason cannot exceed 2000 characters")
    private String claimReason;

    @Size(max = 140, message = "Beneficiary contact cannot exceed 140 characters")
    private String beneficiaryContact;

    private LocalDate processingDeadline;

    @Size(max = 2000, message = "Processing notes cannot exceed 2000 characters")
    private String processingNotes;

    private Boolean requiresSpecialApproval = false;

    private Boolean documentsSubmitted = false;

    // Constructors
    public ClaimCreateRequest() {}

    // Getters and Setters
    public String getClaimReference() {
        return claimReference;
    }

    public void setClaimReference(String claimReference) {
        this.claimReference = claimReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }

    public String getClaimReason() {
        return claimReason;
    }

    public void setClaimReason(String claimReason) {
        this.claimReason = claimReason;
    }

    public String getBeneficiaryContact() {
        return beneficiaryContact;
    }

    public void setBeneficiaryContact(String beneficiaryContact) {
        this.beneficiaryContact = beneficiaryContact;
    }

    public LocalDate getProcessingDeadline() {
        return processingDeadline;
    }

    public void setProcessingDeadline(LocalDate processingDeadline) {
        this.processingDeadline = processingDeadline;
    }

    public String getProcessingNotes() {
        return processingNotes;
    }

    public void setProcessingNotes(String processingNotes) {
        this.processingNotes = processingNotes;
    }

    public Boolean getRequiresSpecialApproval() {
        return requiresSpecialApproval;
    }

    public void setRequiresSpecialApproval(Boolean requiresSpecialApproval) {
        this.requiresSpecialApproval = requiresSpecialApproval;
    }

    public Boolean getDocumentsSubmitted() {
        return documentsSubmitted;
    }

    public void setDocumentsSubmitted(Boolean documentsSubmitted) {
        this.documentsSubmitted = documentsSubmitted;
    }
}




