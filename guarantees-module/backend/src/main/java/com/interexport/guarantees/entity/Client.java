package com.interexport.guarantees.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Entity representing clients/applicants for guarantees.
 * Implements requirements from F10 - Parameters (clients).
 */
@Entity
@Table(name = "clients", indexes = {
    @Index(name = "idx_client_code", columnList = "clientCode", unique = true),
    @Index(name = "idx_client_name", columnList = "name")
})
public class Client extends BaseEntity {

    /**
     * Unique client code/identifier
     */
    @Column(name = "client_code", nullable = false, unique = true, length = 20)
    @NotBlank
    @Size(max = 20)
    private String clientCode;

    /**
     * Client name
     */
    @Column(name = "name", nullable = false, length = 140)
    @NotBlank
    @Size(max = 140)
    private String name;

    /**
     * Client address
     */
    @Column(name = "address", columnDefinition = "TEXT")
    @Size(max = 500)
    private String address;

    /**
     * City
     */
    @Column(name = "city", length = 100)
    @Size(max = 100)
    private String city;

    /**
     * Country code (ISO 3166-1 alpha-2)
     */
    @Column(name = "country_code", length = 2)
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Z]{2}", message = "Country code must be a valid ISO 3166-1 alpha-2 code")
    private String countryCode;

    /**
     * Postal code
     */
    @Column(name = "postal_code", length = 20)
    @Size(max = 20)
    private String postalCode;

    /**
     * Primary contact phone
     */
    @Column(name = "phone", length = 50)
    @Size(max = 50)
    private String phone;

    /**
     * Primary contact email
     */
    @Column(name = "email", length = 100)
    @Size(max = 100)
    @Email
    private String email;

    /**
     * Tax identification number
     */
    @Column(name = "tax_id", length = 50)
    @Size(max = 50)
    private String taxId;

    /**
     * Legal entity type (CORPORATION, PARTNERSHIP, INDIVIDUAL, etc.)
     */
    @Column(name = "entity_type", length = 50)
    @Size(max = 50)
    private String entityType;

    /**
     * Industry sector code
     */
    @Column(name = "industry_code", length = 10)
    @Size(max = 10)
    private String industryCode;

    /**
     * Client risk rating (LOW, MEDIUM, HIGH)
     */
    @Column(name = "risk_rating", length = 20)
    @Size(max = 20)
    private String riskRating;

    /**
     * Credit limit for guarantees
     */
    @Column(name = "credit_limit", precision = 19, scale = 2)
    @DecimalMin(value = "0.00")
    private java.math.BigDecimal creditLimit;

    /**
     * Currency for credit limit
     */
    @Column(name = "credit_currency", length = 3)
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid ISO 4217 code")
    private String creditCurrency;

    /**
     * Whether the client is currently active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * KYC (Know Your Customer) completion date
     */
    @Column(name = "kyc_date")
    private java.time.LocalDate kycDate;

    /**
     * KYC review due date
     */
    @Column(name = "kyc_review_date")
    private java.time.LocalDate kycReviewDate;

    /**
     * Additional notes about the client
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    @Size(max = 1000)
    private String notes;

    // Constructors
    public Client() {}

    public Client(String clientCode, String name) {
        this.clientCode = clientCode;
        this.name = name;
    }

    public Client(String clientCode, String name, String address, String countryCode) {
        this.clientCode = clientCode;
        this.name = name;
        this.address = address;
        this.countryCode = countryCode;
    }

    // Business methods

    /**
     * Check if client has available credit for a guarantee amount
     */
    public boolean hasAvailableCredit(java.math.BigDecimal requestedAmount) {
        if (creditLimit == null || requestedAmount == null) {
            return true; // No limit set or amount not specified
        }
        return creditLimit.compareTo(requestedAmount) >= 0;
    }

    /**
     * Check if KYC review is due
     */
    public boolean isKycReviewDue() {
        return kycReviewDate != null && java.time.LocalDate.now().isAfter(kycReviewDate);
    }

    /**
     * Check if client is eligible for guarantees
     */
    public boolean isEligibleForGuarantees() {
        return isActive && kycDate != null && !isKycReviewDue();
    }

    /**
     * Get full display name with code
     */
    public String getDisplayName() {
        return clientCode + " - " + name;
    }

    // Getters and Setters

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public String getRiskRating() {
        return riskRating;
    }

    public void setRiskRating(String riskRating) {
        this.riskRating = riskRating;
    }

    public java.math.BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(java.math.BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public String getCreditCurrency() {
        return creditCurrency;
    }

    public void setCreditCurrency(String creditCurrency) {
        this.creditCurrency = creditCurrency;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public java.time.LocalDate getKycDate() {
        return kycDate;
    }

    public void setKycDate(java.time.LocalDate kycDate) {
        this.kycDate = kycDate;
    }

    public java.time.LocalDate getKycReviewDate() {
        return kycReviewDate;
    }

    public void setKycReviewDate(java.time.LocalDate kycReviewDate) {
        this.kycReviewDate = kycReviewDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
