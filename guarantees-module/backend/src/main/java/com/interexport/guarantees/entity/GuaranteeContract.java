package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

/**
 * Core entity representing a Guarantee Contract.
 * Implements requirements from F1 - Guarantees CRUD and F15 - Multi-currency Capability.
 */
@Entity
@Table(name = "guarantee_contracts", indexes = {
    @Index(name = "idx_guarantee_reference", columnList = "reference"),
    @Index(name = "idx_guarantee_status", columnList = "status"),
    @Index(name = "idx_guarantee_expiry", columnList = "expiryDate"),
    @Index(name = "idx_guarantee_applicant", columnList = "applicantId")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class GuaranteeContract extends BaseEntity {

    /**
     * Unique business reference for the guarantee (SWIFT field 20)
     */
    @Column(name = "reference", unique = true, nullable = false, length = 35)
    @NotBlank
    @Size(max = 35)
    private String reference;

    /**
     * Guarantee type based on SWIFT field 22A
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "guarantee_type", nullable = false)
    @NotNull
    private GuaranteeType guaranteeType;

    /**
     * Current status of the guarantee
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private GuaranteeStatus status = GuaranteeStatus.DRAFT;

    /**
     * Principal amount of the guarantee
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Currency of the guarantee (ISO 4217)
     */
    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    /**
     * Equivalent amount in base currency (USD)
     */
    @Column(name = "base_amount", precision = 19, scale = 2)
    private BigDecimal baseAmount;

    /**
     * Exchange rate used for base amount calculation
     */
    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    /**
     * Issue date of the guarantee
     */
    @Column(name = "issue_date", nullable = false)
    @NotNull
    private LocalDate issueDate;

    /**
     * Expiry date of the guarantee
     */
    @Column(name = "expiry_date", nullable = false)
    @NotNull
    private LocalDate expiryDate;

    /**
     * Guarantee text content
     */
    @Column(name = "guarantee_text", columnDefinition = "TEXT")
    @Size(max = 10000)
    private String guaranteeText;

    /**
     * Applicant (client) identifier
     */
    @Column(name = "applicant_id", nullable = false)
    @NotNull
    private Long applicantId;

    /**
     * Applicant name (for display purposes)
     */
    @Column(name = "applicant_name", length = 140)
    @Size(max = 140)
    private String applicantName;

    /**
     * SWIFT message reference (for tracking)
     */
    @Column(name = "swift_message_reference", length = 35)
    @Size(max = 35)
    private String swiftMessageReference;

    /**
     * Beneficiary information
     */
    @Column(name = "beneficiary_name", nullable = false, length = 140)
    @NotBlank
    @Size(max = 140)
    private String beneficiaryName;

    @Column(name = "beneficiary_address", length = 350)
    @Size(max = 350)
    private String beneficiaryAddress;

    /**
     * Advising bank BIC code (SWIFT field 57A)
     */
    @Column(name = "advising_bank_bic", length = 11)
    @Size(max = 11)
    private String advisingBankBic;

    /**
     * Flag indicating if this is a domestic guarantee
     */
    @Column(name = "is_domestic", nullable = false)
    private Boolean isDomestic = false;

    /**
     * Underlying contract reference
     */
    @Column(name = "underlying_contract_ref", length = 35)
    @Size(max = 35)
    private String underlyingContractRef;

    /**
     * Additional conditions or special instructions
     */
    @Column(name = "special_conditions", columnDefinition = "TEXT")
    @Size(max = 2000)
    private String specialConditions;

    /**
     * Language for guarantee text rendering
     */
    @Column(name = "language", length = 2)
    @Size(max = 2)
    private String language = "EN";

    // Relationships

    /**
     * Amendments associated with this guarantee
     */
    @OneToMany(mappedBy = "guarantee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("guarantee-amendments")
    private List<Amendment> amendments = new ArrayList<>();

    /**
     * Claims associated with this guarantee
     */
    @OneToMany(mappedBy = "guarantee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("guarantee-claims")
    private List<Claim> claims = new ArrayList<>();

    /**
     * Fee items associated with this guarantee
     */
    @OneToMany(mappedBy = "guarantee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("guarantee-feeitems")
    private List<FeeItem> feeItems = new ArrayList<>();

    // Constructors
    public GuaranteeContract() {}

    public GuaranteeContract(String reference, GuaranteeType guaranteeType, 
                           BigDecimal amount, String currency, 
                           LocalDate issueDate, LocalDate expiryDate,
                           Long applicantId, String beneficiaryName) {
        this.reference = reference;
        this.guaranteeType = guaranteeType;
        this.amount = amount;
        this.currency = currency;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.applicantId = applicantId;
        this.beneficiaryName = beneficiaryName;
    }

    // Business methods
    
    /**
     * Check if the guarantee has expired
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Check if the guarantee has active claims
     */
    public boolean hasActiveClaims() {
        return claims.stream().anyMatch(claim -> !claim.getStatus().equals(
            com.interexport.guarantees.entity.enums.ClaimStatus.SETTLED) && 
            !claim.getStatus().equals(com.interexport.guarantees.entity.enums.ClaimStatus.REJECTED));
    }

    /**
     * Calculate days until expiry
     */
    public long getDaysUntilExpiry() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    /**
     * Get total claimed amount
     */
    public BigDecimal getTotalClaimedAmount() {
        return claims.stream()
                .filter(claim -> claim.getStatus().equals(
                    com.interexport.guarantees.entity.enums.ClaimStatus.SETTLED))
                .map(Claim::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get remaining guarantee amount (original - claimed)
     */
    public BigDecimal getRemainingAmount() {
        return amount.subtract(getTotalClaimedAmount());
    }

    // Getters and Setters

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public GuaranteeType getGuaranteeType() {
        return guaranteeType;
    }

    public void setGuaranteeType(GuaranteeType guaranteeType) {
        this.guaranteeType = guaranteeType;
    }

    public GuaranteeStatus getStatus() {
        return status;
    }

    public void setStatus(GuaranteeStatus status) {
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

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getGuaranteeText() {
        return guaranteeText;
    }

    public void setGuaranteeText(String guaranteeText) {
        this.guaranteeText = guaranteeText;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryAddress() {
        return beneficiaryAddress;
    }

    public void setBeneficiaryAddress(String beneficiaryAddress) {
        this.beneficiaryAddress = beneficiaryAddress;
    }

    public String getAdvisingBankBic() {
        return advisingBankBic;
    }

    public void setAdvisingBankBic(String advisingBankBic) {
        this.advisingBankBic = advisingBankBic;
    }

    public Boolean getIsDomestic() {
        return isDomestic;
    }

    public void setIsDomestic(Boolean isDomestic) {
        this.isDomestic = isDomestic;
    }

    public String getUnderlyingContractRef() {
        return underlyingContractRef;
    }

    public void setUnderlyingContractRef(String underlyingContractRef) {
        this.underlyingContractRef = underlyingContractRef;
    }

    public String getSpecialConditions() {
        return specialConditions;
    }

    public void setSpecialConditions(String specialConditions) {
        this.specialConditions = specialConditions;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<Amendment> getAmendments() {
        return amendments;
    }

    public void setAmendments(List<Amendment> amendments) {
        this.amendments = amendments;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }

    public List<FeeItem> getFeeItems() {
        return feeItems;
    }

    public void setFeeItems(List<FeeItem> feeItems) {
        this.feeItems = feeItems;
    }
}
