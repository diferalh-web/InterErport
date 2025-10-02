package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.GuaranteeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Commission Parameter entity for Parameters Module (F10)
 * Defines commission calculation rules for different guarantee types and scenarios
 * 
 * Business Rules:
 * - Commission rates are percentage-based (0.01 = 1%)
 * - Minimum amounts ensure profitability
 * - Maximum amounts cap exposure
 * - Rules can be currency-specific
 * - Different rules for domestic vs international
 */
@Entity
@Table(name = "commission_parameters", indexes = {
    @Index(name = "idx_commission_type", columnList = "guaranteeType"),
    @Index(name = "idx_commission_currency", columnList = "currency"),
    @Index(name = "idx_commission_domestic", columnList = "isDomestic"),
    @Index(name = "idx_commission_active", columnList = "isActive")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_commission_rule", 
        columnNames = {"guaranteeType", "currency", "isDomestic", "clientSegment"})
})
public class CommissionParameter extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "guarantee_type", nullable = false, length = 20)
    @NotNull(message = "Guarantee type is required")
    private GuaranteeType guaranteeType;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "is_domestic", nullable = false)
    private Boolean isDomestic = true;

    @Size(max = 50, message = "Client segment cannot exceed 50 characters")
    @Column(name = "client_segment", length = 50)
    private String clientSegment = "STANDARD";

    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.0", message = "Commission rate cannot be negative")
    @DecimalMax(value = "1.0", message = "Commission rate cannot exceed 100%")
    @Column(name = "commission_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal commissionRate;

    @DecimalMin(value = "0.0", message = "Minimum amount cannot be negative")
    @Column(name = "minimum_amount", precision = 19, scale = 4)
    private BigDecimal minimumAmount = BigDecimal.ZERO;

    @Column(name = "maximum_amount", precision = 19, scale = 4)
    private BigDecimal maximumAmount;

    @DecimalMin(value = "1", message = "Default installments must be at least 1")
    @Column(name = "default_installments", nullable = false)
    private Integer defaultInstallments = 1;

    @Column(name = "allow_manual_distribution", nullable = false)
    private Boolean allowManualDistribution = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_basis", nullable = false, length = 20)
    private CalculationBasis calculationBasis = CalculationBasis.FULL_AMOUNT;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "effective_from")
    private java.time.LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private java.time.LocalDate effectiveTo;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    // Constructors
    public CommissionParameter() {}

    public CommissionParameter(GuaranteeType guaranteeType, String currency, 
                             BigDecimal commissionRate, Boolean isDomestic) {
        this.guaranteeType = guaranteeType;
        this.currency = currency;
        this.commissionRate = commissionRate;
        this.isDomestic = isDomestic;
        this.effectiveFrom = java.time.LocalDate.now();
    }

    // Getters and Setters
    public GuaranteeType getGuaranteeType() { return guaranteeType; }
    public void setGuaranteeType(GuaranteeType guaranteeType) { this.guaranteeType = guaranteeType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency != null ? currency.toUpperCase() : null; }

    public Boolean getIsDomestic() { return isDomestic; }
    public void setIsDomestic(Boolean isDomestic) { this.isDomestic = isDomestic; }

    public String getClientSegment() { return clientSegment; }
    public void setClientSegment(String clientSegment) { this.clientSegment = clientSegment; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }

    public BigDecimal getMinimumAmount() { return minimumAmount; }
    public void setMinimumAmount(BigDecimal minimumAmount) { this.minimumAmount = minimumAmount; }

    public BigDecimal getMaximumAmount() { return maximumAmount; }
    public void setMaximumAmount(BigDecimal maximumAmount) { this.maximumAmount = maximumAmount; }

    public Integer getDefaultInstallments() { return defaultInstallments; }
    public void setDefaultInstallments(Integer defaultInstallments) { this.defaultInstallments = defaultInstallments; }

    public Boolean getAllowManualDistribution() { return allowManualDistribution; }
    public void setAllowManualDistribution(Boolean allowManualDistribution) { this.allowManualDistribution = allowManualDistribution; }

    public CalculationBasis getCalculationBasis() { return calculationBasis; }
    public void setCalculationBasis(CalculationBasis calculationBasis) { this.calculationBasis = calculationBasis; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public java.time.LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(java.time.LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public java.time.LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(java.time.LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Helper methods
    public boolean isValidForDate(java.time.LocalDate date) {
        boolean afterStart = effectiveFrom == null || !date.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);
        return isActive && afterStart && beforeEnd;
    }

    public BigDecimal getCommissionRateAsPercentage() {
        return commissionRate.multiply(BigDecimal.valueOf(100));
    }

    public String getDisplayName() {
        return String.format("%s %s (%s) - %.4f%%", 
                guaranteeType.name(), currency, 
                isDomestic ? "Domestic" : "International", 
                getCommissionRateAsPercentage());
    }

    @Override
    public String toString() {
        return String.format("CommissionParameter{id=%d, type=%s, currency='%s', rate=%.4f%%, domestic=%s, active=%s}", 
                getId(), guaranteeType, currency, getCommissionRateAsPercentage(), isDomestic, isActive);
    }

    /**
     * Calculation Basis for commission computation
     */
    public enum CalculationBasis {
        FULL_AMOUNT("Full Guarantee Amount"),
        UTILIZED_AMOUNT("Utilized Amount Only"),
        OUTSTANDING_AMOUNT("Outstanding Amount"),
        TIME_BASED("Time-based Calculation");

        private final String displayName;

        CalculationBasis(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }
}





