package com.interexport.guarantees.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Entity representing fee items and commission calculations for guarantees.
 * Implements requirements from F3 - Commission and Exchange Rate Calculation.
 */
@Entity
@Table(name = "fee_items", indexes = {
    @Index(name = "idx_fee_guarantee", columnList = "guarantee_id"),
    @Index(name = "idx_fee_type", columnList = "feeType"),
    @Index(name = "idx_fee_due_date", columnList = "dueDate")
})
public class FeeItem extends BaseEntity {

    /**
     * Reference to the parent guarantee
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guarantee_id", nullable = false)
    @NotNull
    @JsonBackReference("guarantee-feeitems")
    private GuaranteeContract guarantee;

    /**
     * Type of fee (COMMISSION, SERVICE_FEE, AMENDMENT_FEE, etc.)
     */
    @Column(name = "fee_type", nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String feeType;

    /**
     * Description of the fee
     */
    @Column(name = "description", length = 200)
    @Size(max = 200)
    private String description;

    /**
     * Fee amount
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull
    @DecimalMin(value = "0.00", message = "Fee amount cannot be negative")
    private BigDecimal amount;

    /**
     * Currency of the fee
     */
    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    /**
     * Fee amount in base currency (USD)
     */
    @Column(name = "base_amount", precision = 19, scale = 2)
    private BigDecimal baseAmount;

    /**
     * Exchange rate used for base amount calculation
     */
    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    /**
     * Date when the fee is due
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Fee calculation rate (percentage)
     */
    @Column(name = "rate", precision = 8, scale = 6)
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal rate;

    /**
     * Minimum fee amount (if applicable)
     */
    @Column(name = "minimum_amount", precision = 19, scale = 2)
    @DecimalMin(value = "0.00")
    private BigDecimal minimumAmount;

    /**
     * Installment number (for deferred payments)
     */
    @Column(name = "installment_number")
    @Min(value = 1)
    private Integer installmentNumber;

    /**
     * Total number of installments
     */
    @Column(name = "total_installments")
    @Min(value = 1)
    private Integer totalInstallments;

    /**
     * Whether this fee has been paid
     */
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    /**
     * Date when the fee was paid
     */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    /**
     * Payment reference
     */
    @Column(name = "payment_reference", length = 35)
    @Size(max = 35)
    private String paymentReference;

    /**
     * Associated accounting entry reference
     */
    @Column(name = "accounting_entry_ref", length = 50)
    @Size(max = 50)
    private String accountingEntryRef;

    /**
     * Flag indicating if this is automatically calculated
     */
    @Column(name = "is_calculated", nullable = false)
    private Boolean isCalculated = true;

    /**
     * Calculation notes or manual adjustment reason
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    @Size(max = 1000)
    private String notes;

    // Constructors
    public FeeItem() {}

    public FeeItem(GuaranteeContract guarantee, String feeType, BigDecimal amount, String currency) {
        this.guarantee = guarantee;
        this.feeType = feeType;
        this.amount = amount;
        this.currency = currency;
    }

    public FeeItem(GuaranteeContract guarantee, String feeType, String description, 
                  BigDecimal amount, String currency, LocalDate dueDate) {
        this.guarantee = guarantee;
        this.feeType = feeType;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.dueDate = dueDate;
    }

    // Business methods

    /**
     * Mark the fee as paid
     */
    public void markAsPaid(String paymentReference, String accountingEntryRef) {
        if (this.isPaid) {
            throw new IllegalStateException("Fee is already marked as paid");
        }
        
        this.isPaid = true;
        this.paymentDate = LocalDate.now();
        this.paymentReference = paymentReference;
        this.accountingEntryRef = accountingEntryRef;
    }

    /**
     * Calculate the fee amount based on guarantee amount and rate
     */
    public void calculateAmount(BigDecimal guaranteeAmount, BigDecimal feeRate, BigDecimal minimumFee) {
        if (guaranteeAmount == null || feeRate == null) {
            throw new IllegalArgumentException("Guarantee amount and fee rate cannot be null");
        }
        
        this.rate = feeRate;
        this.minimumAmount = minimumFee;
        
        // Calculate percentage-based fee
        BigDecimal calculatedAmount = guaranteeAmount.multiply(feeRate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        
        // Apply minimum if specified
        if (minimumFee != null && calculatedAmount.compareTo(minimumFee) < 0) {
            calculatedAmount = minimumFee;
        }
        
        this.amount = calculatedAmount;
        this.isCalculated = true;
    }

    /**
     * Convert amount to base currency
     */
    public void convertToBaseCurrency(BigDecimal exchangeRate) {
        if (exchangeRate == null) {
            throw new IllegalArgumentException("Exchange rate cannot be null");
        }
        
        this.exchangeRate = exchangeRate;
        this.baseAmount = this.amount.multiply(exchangeRate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Check if the fee is overdue
     */
    public boolean isOverdue() {
        return !isPaid && dueDate != null && LocalDate.now().isAfter(dueDate);
    }

    /**
     * Get days until due date (negative if overdue)
     */
    public Long getDaysUntilDue() {
        if (dueDate == null) return null;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /**
     * Create installment fees for deferred payment
     */
    public static FeeItem createInstallment(FeeItem parent, int installmentNumber, 
                                          int totalInstallments, BigDecimal installmentAmount, 
                                          LocalDate installmentDueDate) {
        FeeItem installment = new FeeItem();
        installment.setGuarantee(parent.getGuarantee());
        installment.setFeeType(parent.getFeeType() + "_INSTALLMENT");
        installment.setDescription(parent.getDescription() + " - Installment " + installmentNumber + "/" + totalInstallments);
        installment.setAmount(installmentAmount);
        installment.setCurrency(parent.getCurrency());
        installment.setDueDate(installmentDueDate);
        installment.setInstallmentNumber(installmentNumber);
        installment.setTotalInstallments(totalInstallments);
        installment.setIsCalculated(true);
        
        return installment;
    }

    // Getters and Setters

    public GuaranteeContract getGuarantee() {
        return guarantee;
    }

    public void setGuarantee(GuaranteeContract guarantee) {
        this.guarantee = guarantee;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public void setMinimumAmount(BigDecimal minimumAmount) {
        this.minimumAmount = minimumAmount;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public Integer getTotalInstallments() {
        return totalInstallments;
    }

    public void setTotalInstallments(Integer totalInstallments) {
        this.totalInstallments = totalInstallments;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getAccountingEntryRef() {
        return accountingEntryRef;
    }

    public void setAccountingEntryRef(String accountingEntryRef) {
        this.accountingEntryRef = accountingEntryRef;
    }

    public Boolean getIsCalculated() {
        return isCalculated;
    }

    public void setIsCalculated(Boolean isCalculated) {
        this.isCalculated = isCalculated;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
