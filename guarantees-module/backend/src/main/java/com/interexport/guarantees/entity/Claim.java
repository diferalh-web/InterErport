package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.ClaimStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Entity representing a Claim against a Guarantee.
 * Implements requirements from F6 - Claims (request, payment, rejection).
 */
@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claim_guarantee", columnList = "guarantee_id"),
    @Index(name = "idx_claim_reference", columnList = "claimReference"),
    @Index(name = "idx_claim_status", columnList = "status")
})
public class Claim extends BaseEntity {

    /**
     * Reference to the parent guarantee
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guarantee_id", nullable = false)
    @NotNull
    @JsonBackReference("guarantee-claims")
    private GuaranteeContract guarantee;

    /**
     * Unique reference for this claim
     */
    @Column(name = "claim_reference", nullable = false, length = 35)
    @NotBlank
    @Size(max = 35)
    private String claimReference;

    /**
     * Current status of the claim
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private ClaimStatus status = ClaimStatus.REQUESTED;

    /**
     * Claimed amount
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull
    @DecimalMin(value = "0.01", message = "Claim amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Currency of the claim (should match guarantee currency)
     */
    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    /**
     * Date when the claim was received
     */
    @Column(name = "claim_date", nullable = false)
    @NotNull
    private LocalDate claimDate;

    /**
     * Deadline for processing the claim
     */
    @Column(name = "processing_deadline")
    private LocalDate processingDeadline;

    /**
     * Reason for the claim
     */
    @Column(name = "claim_reason", columnDefinition = "TEXT")
    @Size(max = 2000)
    private String claimReason;

    /**
     * Details of the documents submitted with the claim
     */
    @Column(name = "documents_submitted", columnDefinition = "TEXT")
    @Size(max = 1000)
    private String documentsSubmitted;

    /**
     * List of missing documents (if any)
     */
    @Column(name = "missing_documents", columnDefinition = "TEXT")
    @Size(max = 1000)
    private String missingDocuments;

    /**
     * Beneficiary contact information
     */
    @Column(name = "beneficiary_contact", length = 200)
    @Size(max = 200)
    private String beneficiaryContact;

    /**
     * Date when the claim was approved
     */
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    /**
     * User who approved the claim
     */
    @Column(name = "approved_by", length = 100)
    @Size(max = 100)
    private String approvedBy;

    /**
     * Date when the claim was paid
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * Payment reference number
     */
    @Column(name = "payment_reference", length = 35)
    @Size(max = 35)
    private String paymentReference;

    /**
     * Date when the claim was rejected
     */
    @Column(name = "rejected_date")
    private LocalDateTime rejectedDate;

    /**
     * User who rejected the claim
     */
    @Column(name = "rejected_by", length = 100)
    @Size(max = 100)
    private String rejectedBy;

    /**
     * Reason for rejection
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    @Size(max = 1000)
    private String rejectionReason;

    /**
     * Additional processing notes
     */
    @Column(name = "processing_notes", columnDefinition = "TEXT")
    @Size(max = 2000)
    private String processingNotes;

    /**
     * Reference to external SWIFT message if applicable (MT765)
     */
    @Column(name = "swift_message_reference", length = 35)
    @Size(max = 35)
    private String swiftMessageReference;

    /**
     * Flag indicating if this claim requires special approval
     */
    @Column(name = "requires_special_approval", nullable = false)
    private Boolean requiresSpecialApproval = false;

    // Relationships

    /**
     * Document attachments for this claim
     */
    @ElementCollection
    @CollectionTable(name = "claim_documents", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "document_path")
    private List<String> documentPaths = new ArrayList<>();

    // Constructors
    public Claim() {}

    public Claim(GuaranteeContract guarantee, String claimReference, 
                BigDecimal amount, String currency, LocalDate claimDate, String claimReason) {
        this.guarantee = guarantee;
        this.claimReference = claimReference;
        this.amount = amount;
        this.currency = currency;
        this.claimDate = claimDate;
        this.claimReason = claimReason;
    }

    // Business methods

    /**
     * Move claim to under review status
     */
    public void startReview() {
        if (this.status != ClaimStatus.REQUESTED) {
            throw new IllegalStateException("Only requested claims can be reviewed");
        }
        this.status = ClaimStatus.UNDER_REVIEW;
    }

    /**
     * Mark claim as pending documents
     */
    public void markPendingDocuments(String missingDocs) {
        if (!this.status.isEditable()) {
            throw new IllegalStateException("Cannot change documents requirement for claim in status: " + this.status);
        }
        this.status = ClaimStatus.PENDING_DOCUMENTS;
        this.missingDocuments = missingDocs;
    }

    /**
     * Approve the claim for payment
     */
    public void approve(String approvedBy) {
        if (this.status != ClaimStatus.UNDER_REVIEW && this.status != ClaimStatus.PENDING_DOCUMENTS) {
            throw new IllegalStateException("Can only approve claims under review or with complete documents");
        }
        if (hasOutstandingDocuments()) {
            throw new IllegalStateException("Cannot approve claim with missing documents");
        }
        
        this.status = ClaimStatus.APPROVED;
        this.approvedDate = LocalDateTime.now();
        this.approvedBy = approvedBy;
    }

    /**
     * Reject the claim
     */
    public void reject(String rejectedBy, String rejectionReason) {
        if (!this.status.isEditable()) {
            throw new IllegalStateException("Cannot reject claim in status: " + this.status);
        }
        
        this.status = ClaimStatus.REJECTED;
        this.rejectedDate = LocalDateTime.now();
        this.rejectedBy = rejectedBy;
        this.rejectionReason = rejectionReason;
    }

    /**
     * Process payment for the claim
     */
    public void processPayment(String paymentReference) {
        if (this.status != ClaimStatus.APPROVED) {
            throw new IllegalStateException("Can only pay approved claims");
        }
        
        this.status = ClaimStatus.SETTLED;
        this.paymentDate = LocalDateTime.now();
        this.paymentReference = paymentReference;
    }

    /**
     * Check if the claim has outstanding document requirements
     */
    public boolean hasOutstandingDocuments() {
        return missingDocuments != null && !missingDocuments.trim().isEmpty();
    }

    /**
     * Validate that claim amount does not exceed guarantee remaining amount
     */
    public void validateAmount() {
        if (guarantee != null) {
            BigDecimal remainingAmount = guarantee.getRemainingAmount();
            if (amount.compareTo(remainingAmount) > 0) {
                throw new IllegalArgumentException(
                    String.format("Claim amount %s exceeds remaining guarantee amount %s", 
                                amount, remainingAmount));
            }
        }
    }

    /**
     * Check if the claim is overdue for processing
     */
    public boolean isOverdue() {
        return processingDeadline != null && LocalDate.now().isAfter(processingDeadline);
    }

    /**
     * Calculate days until processing deadline
     */
    public Long getDaysUntilDeadline() {
        if (processingDeadline == null) return null;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), processingDeadline);
    }

    // Getters and Setters

    public GuaranteeContract getGuarantee() {
        return guarantee;
    }

    public void setGuarantee(GuaranteeContract guarantee) {
        this.guarantee = guarantee;
    }

    public String getClaimReference() {
        return claimReference;
    }

    public void setClaimReference(String claimReference) {
        this.claimReference = claimReference;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
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

    public LocalDate getProcessingDeadline() {
        return processingDeadline;
    }

    public void setProcessingDeadline(LocalDate processingDeadline) {
        this.processingDeadline = processingDeadline;
    }

    public String getClaimReason() {
        return claimReason;
    }

    public void setClaimReason(String claimReason) {
        this.claimReason = claimReason;
    }

    public String getDocumentsSubmitted() {
        return documentsSubmitted;
    }

    public void setDocumentsSubmitted(String documentsSubmitted) {
        this.documentsSubmitted = documentsSubmitted;
    }

    public String getMissingDocuments() {
        return missingDocuments;
    }

    public void setMissingDocuments(String missingDocuments) {
        this.missingDocuments = missingDocuments;
    }

    public String getBeneficiaryContact() {
        return beneficiaryContact;
    }

    public void setBeneficiaryContact(String beneficiaryContact) {
        this.beneficiaryContact = beneficiaryContact;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LocalDateTime getRejectedDate() {
        return rejectedDate;
    }

    public void setRejectedDate(LocalDateTime rejectedDate) {
        this.rejectedDate = rejectedDate;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getProcessingNotes() {
        return processingNotes;
    }

    public void setProcessingNotes(String processingNotes) {
        this.processingNotes = processingNotes;
    }

    public String getSwiftMessageReference() {
        return swiftMessageReference;
    }

    public void setSwiftMessageReference(String swiftMessageReference) {
        this.swiftMessageReference = swiftMessageReference;
    }

    public Boolean getRequiresSpecialApproval() {
        return requiresSpecialApproval;
    }

    public void setRequiresSpecialApproval(Boolean requiresSpecialApproval) {
        this.requiresSpecialApproval = requiresSpecialApproval;
    }

    public List<String> getDocumentPaths() {
        return documentPaths;
    }

    public void setDocumentPaths(List<String> documentPaths) {
        this.documentPaths = documentPaths;
    }
}
