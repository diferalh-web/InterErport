package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.AmendmentType;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Entity representing an Amendment to a Guarantee.
 * Implements requirements from F5 - Amendments (immediate and with consent).
 */
@Entity
@Table(name = "amendments", indexes = {
    @Index(name = "idx_amendment_guarantee", columnList = "guarantee_id"),
    @Index(name = "idx_amendment_reference", columnList = "amendmentReference"),
    @Index(name = "idx_amendment_status", columnList = "status")
})
public class Amendment extends BaseEntity {

    /**
     * Reference to the parent guarantee
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guarantee_id", nullable = false)
    @NotNull
    @JsonBackReference("guarantee-amendments")
    private GuaranteeContract guarantee;

    /**
     * Unique reference for this amendment
     */
    @Column(name = "amendment_reference", nullable = false, length = 35)
    @NotBlank
    @Size(max = 35)
    private String amendmentReference;

    /**
     * Type of amendment (with or without consent)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "amendment_type", nullable = false)
    @NotNull
    private AmendmentType amendmentType;

    /**
     * Current status of the amendment
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private GuaranteeStatus status = GuaranteeStatus.DRAFT;

    /**
     * JSON representation of the proposed changes
     */
    @Column(name = "changes_json", columnDefinition = "TEXT", nullable = false)
    @NotBlank
    private String changesJson;

    /**
     * Human-readable description of the changes
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 2000)
    private String description;

    /**
     * Reason for the amendment
     */
    @Column(name = "reason", length = 500)
    @Size(max = 500)
    private String reason;

    /**
     * Date when the amendment was submitted
     */
    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    /**
     * Date when the amendment was approved/rejected
     */
    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    /**
     * User who processed the amendment
     */
    @Column(name = "processed_by", length = 100)
    @Size(max = 100)
    private String processedBy;

    /**
     * Comments from the processor
     */
    @Column(name = "processing_comments", columnDefinition = "TEXT")
    @Size(max = 1000)
    private String processingComments;

    /**
     * Whether consent from beneficiary is required
     */
    @Column(name = "requires_consent", nullable = false)
    private Boolean requiresConsent = false;

    /**
     * Date when consent was received (if applicable)
     */
    @Column(name = "consent_received_date")
    private LocalDateTime consentReceivedDate;

    /**
     * Reference to external SWIFT message if applicable
     */
    @Column(name = "swift_message_reference", length = 35)
    @Size(max = 35)
    private String swiftMessageReference;

    // Constructors
    public Amendment() {}

    public Amendment(GuaranteeContract guarantee, String amendmentReference, 
                    AmendmentType amendmentType, String changesJson, String description) {
        this.guarantee = guarantee;
        this.amendmentReference = amendmentReference;
        this.amendmentType = amendmentType;
        this.changesJson = changesJson;
        this.description = description;
        // Determine if consent is required based on amendment type
        this.requiresConsent = requiresConsentForAmendmentType(amendmentType);
    }

    // Business methods

    /**
     * Determine if consent is required for a given amendment type
     */
    private boolean requiresConsentForAmendmentType(AmendmentType amendmentType) {
        if (amendmentType == null) return false;
        
        // Business rule: Amount increases, beneficiary changes, and currency changes typically require consent
        return amendmentType == AmendmentType.AMOUNT_INCREASE ||
               amendmentType == AmendmentType.CHANGE_BENEFICIARY ||
               amendmentType == AmendmentType.CHANGE_CURRENCY;
    }

    /**
     * Submit the amendment for processing
     */
    public void submit() {
        if (this.status != GuaranteeStatus.DRAFT) {
            throw new IllegalStateException("Only draft amendments can be submitted");
        }
        this.status = GuaranteeStatus.SUBMITTED;
        this.submittedDate = LocalDateTime.now();
    }

    /**
     * Approve the amendment
     */
    public void approve(String processedBy, String comments) {
        if (this.status != GuaranteeStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted amendments can be approved");
        }
        if (requiresConsent && consentReceivedDate == null) {
            throw new IllegalStateException("Consent is required but not received");
        }
        
        this.status = GuaranteeStatus.APPROVED;
        this.processedDate = LocalDateTime.now();
        this.processedBy = processedBy;
        this.processingComments = comments;
    }

    /**
     * Reject the amendment
     */
    public void reject(String processedBy, String comments) {
        if (this.status != GuaranteeStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted amendments can be rejected");
        }
        
        this.status = GuaranteeStatus.REJECTED;
        this.processedDate = LocalDateTime.now();
        this.processedBy = processedBy;
        this.processingComments = comments;
    }

    /**
     * Mark consent as received
     */
    public void receiveConsent() {
        if (!requiresConsent) {
            throw new IllegalStateException("This amendment does not require consent");
        }
        this.consentReceivedDate = LocalDateTime.now();
    }

    /**
     * Check if the amendment is editable
     */
    public boolean isEditable() {
        return status.isEditable();
    }

    /**
     * Check if the amendment can be processed
     */
    public boolean canProcess() {
        return status == GuaranteeStatus.SUBMITTED && 
               (!requiresConsent || consentReceivedDate != null);
    }

    // Getters and Setters

    public GuaranteeContract getGuarantee() {
        return guarantee;
    }

    public void setGuarantee(GuaranteeContract guarantee) {
        this.guarantee = guarantee;
    }

    public String getAmendmentReference() {
        return amendmentReference;
    }

    public void setAmendmentReference(String amendmentReference) {
        this.amendmentReference = amendmentReference;
    }

    public AmendmentType getAmendmentType() {
        return amendmentType;
    }

    public void setAmendmentType(AmendmentType amendmentType) {
        this.amendmentType = amendmentType;
        // Determine if consent is required based on amendment type
        this.requiresConsent = requiresConsentForAmendmentType(amendmentType);
    }

    public GuaranteeStatus getStatus() {
        return status;
    }

    public void setStatus(GuaranteeStatus status) {
        this.status = status;
    }

    public String getChangesJson() {
        return changesJson;
    }

    public void setChangesJson(String changesJson) {
        this.changesJson = changesJson;
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

    public LocalDateTime getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDateTime submittedDate) {
        this.submittedDate = submittedDate;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getProcessingComments() {
        return processingComments;
    }

    public void setProcessingComments(String processingComments) {
        this.processingComments = processingComments;
    }

    public Boolean getRequiresConsent() {
        return requiresConsent;
    }

    public void setRequiresConsent(Boolean requiresConsent) {
        this.requiresConsent = requiresConsent;
    }

    public LocalDateTime getConsentReceivedDate() {
        return consentReceivedDate;
    }

    public void setConsentReceivedDate(LocalDateTime consentReceivedDate) {
        this.consentReceivedDate = consentReceivedDate;
    }

    public String getSwiftMessageReference() {
        return swiftMessageReference;
    }

    public void setSwiftMessageReference(String swiftMessageReference) {
        this.swiftMessageReference = swiftMessageReference;
    }
}
