package com.interexport.guarantees.dto;

import com.interexport.guarantees.entity.enums.GuaranteeType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new guarantee
 * Maps from frontend GuaranteeFormData to backend GuaranteeContract
 */
public class GuaranteeCreateRequest {
    
    @NotBlank(message = "Reference is required")
    private String reference;
    
    @NotNull(message = "Guarantee type is required")
    private GuaranteeType guaranteeType;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;
    
    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;
    
    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;
    
    @NotNull(message = "Applicant ID is required")
    private Long applicantId;
    
    @NotBlank(message = "Beneficiary name is required")
    private String beneficiaryName;
    
    private String beneficiaryAddress;
    private String advisingBankBic;
    private Boolean isDomestic = false;
    private String underlyingContractRef;
    private String specialConditions;
    private String guaranteeText;
    private String language = "EN";
    
    // Constructors
    public GuaranteeCreateRequest() {}
    
    // Getters and Setters
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
    public GuaranteeType getGuaranteeType() { return guaranteeType; }
    public void setGuaranteeType(GuaranteeType guaranteeType) { this.guaranteeType = guaranteeType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    
    public String getBeneficiaryAddress() { return beneficiaryAddress; }
    public void setBeneficiaryAddress(String beneficiaryAddress) { this.beneficiaryAddress = beneficiaryAddress; }
    
    public String getAdvisingBankBic() { return advisingBankBic; }
    public void setAdvisingBankBic(String advisingBankBic) { this.advisingBankBic = advisingBankBic; }
    
    public Boolean getIsDomestic() { return isDomestic; }
    public void setIsDomestic(Boolean isDomestic) { this.isDomestic = isDomestic; }
    
    public String getUnderlyingContractRef() { return underlyingContractRef; }
    public void setUnderlyingContractRef(String underlyingContractRef) { this.underlyingContractRef = underlyingContractRef; }
    
    public String getSpecialConditions() { return specialConditions; }
    public void setSpecialConditions(String specialConditions) { this.specialConditions = specialConditions; }
    
    public String getGuaranteeText() { return guaranteeText; }
    public void setGuaranteeText(String guaranteeText) { this.guaranteeText = guaranteeText; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
