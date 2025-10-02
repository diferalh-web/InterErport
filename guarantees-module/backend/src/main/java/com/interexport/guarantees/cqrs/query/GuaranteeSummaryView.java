package com.interexport.guarantees.cqrs.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Denormalized view for guarantee summaries
 * Optimized for dashboard and list queries
 */
public class GuaranteeSummaryView {
    
    private String guaranteeId;
    private String reference;
    private String guaranteeType;
    private BigDecimal amount;
    private String currency;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String beneficiaryName;
    private String applicantName; // Denormalized from applicant table
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional denormalized fields for better query performance
    private String currencySymbol;
    private BigDecimal amountInUsd; // Pre-calculated for reporting
    private Integer daysToExpiry;
    private String riskLevel; // Calculated field
    private String region; // Denormalized from applicant
    
    // Constructors
    public GuaranteeSummaryView() {}
    
    public GuaranteeSummaryView(String guaranteeId, String reference, String guaranteeType,
                              BigDecimal amount, String currency, LocalDate issueDate,
                              LocalDate expiryDate, String beneficiaryName, String applicantName,
                              String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.guaranteeId = guaranteeId;
        this.reference = reference;
        this.guaranteeType = guaranteeType;
        this.amount = amount;
        this.currency = currency;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.beneficiaryName = beneficiaryName;
        this.applicantName = applicantName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public String getGuaranteeId() { return guaranteeId; }
    public void setGuaranteeId(String guaranteeId) { this.guaranteeId = guaranteeId; }
    
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
    public String getGuaranteeType() { return guaranteeType; }
    public void setGuaranteeType(String guaranteeType) { this.guaranteeType = guaranteeType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }
    
    public BigDecimal getAmountInUsd() { return amountInUsd; }
    public void setAmountInUsd(BigDecimal amountInUsd) { this.amountInUsd = amountInUsd; }
    
    public Integer getDaysToExpiry() { return daysToExpiry; }
    public void setDaysToExpiry(Integer daysToExpiry) { this.daysToExpiry = daysToExpiry; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
