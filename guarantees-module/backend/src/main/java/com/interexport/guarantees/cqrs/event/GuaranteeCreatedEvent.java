package com.interexport.guarantees.cqrs.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event published when a guarantee is created
 * Used for eventual consistency between command and query sides
 */
public class GuaranteeCreatedEvent {
    
    private String guaranteeId;
    private String reference;
    private String guaranteeType;
    private BigDecimal amount;
    private String currency;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String beneficiaryName;
    private Long applicantId;
    private String guaranteeText;
    private String language;
    private String status;
    private LocalDateTime createdAt;
    private String eventId;
    private LocalDateTime eventTimestamp;
    
    // Constructors
    public GuaranteeCreatedEvent() {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventTimestamp = LocalDateTime.now();
    }
    
    public GuaranteeCreatedEvent(String guaranteeId, String reference, String guaranteeType,
                               BigDecimal amount, String currency, LocalDate issueDate,
                               LocalDate expiryDate, String beneficiaryName, Long applicantId,
                               String guaranteeText, String language, String status, LocalDateTime createdAt) {
        this();
        this.guaranteeId = guaranteeId;
        this.reference = reference;
        this.guaranteeType = guaranteeType;
        this.amount = amount;
        this.currency = currency;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.beneficiaryName = beneficiaryName;
        this.applicantId = applicantId;
        this.guaranteeText = guaranteeText;
        this.language = language;
        this.status = status;
        this.createdAt = createdAt;
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
    
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    
    public String getGuaranteeText() { return guaranteeText; }
    public void setGuaranteeText(String guaranteeText) { this.guaranteeText = guaranteeText; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
