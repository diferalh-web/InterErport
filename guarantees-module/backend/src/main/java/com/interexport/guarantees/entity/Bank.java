package com.interexport.guarantees.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Bank entity for Parameters Module (F10)
 * Represents correspondent banks and internal bank branches
 * 
 * Business Rules:
 * - BIC/SWIFT codes must be valid format (8 or 11 characters)
 * - Bank names must be unique within the system
 * - Active banks can be used for new guarantees
 * - Correspondent banks handle international guarantees
 */
@Entity
@Table(name = "banks", indexes = {
    @Index(name = "idx_bank_bic", columnList = "bicCode"),
    @Index(name = "idx_bank_name", columnList = "name"),
    @Index(name = "idx_bank_country", columnList = "countryCode")
})
public class Bank extends BaseEntity {

    @NotBlank(message = "Bank name is required")
    @Size(min = 2, max = 200, message = "Bank name must be between 2 and 200 characters")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank(message = "BIC code is required")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", 
             message = "BIC code must be 8 or 11 characters in valid SWIFT format")
    @Column(name = "bic_code", nullable = false, unique = true, length = 11)
    private String bicCode;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Column(name = "address", length = 500)
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Column(name = "city", length = 100)
    private String city;

    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be 2-letter ISO code")
    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    @Column(name = "phone", length = 50)
    private String phone;

    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "is_correspondent", nullable = false)
    private Boolean isCorrespondent = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_type", length = 20)
    private BankType bankType = BankType.CORRESPONDENT;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    // Relationships
    @OneToMany(mappedBy = "bank", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts;

    // Constructors
    public Bank() {}

    public Bank(String name, String bicCode, String countryCode) {
        this.name = name;
        this.bicCode = bicCode;
        this.countryCode = countryCode;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBicCode() { return bicCode; }
    public void setBicCode(String bicCode) { this.bicCode = bicCode != null ? bicCode.toUpperCase() : null; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode != null ? countryCode.toUpperCase() : null; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getIsCorrespondent() { return isCorrespondent; }
    public void setIsCorrespondent(Boolean isCorrespondent) { this.isCorrespondent = isCorrespondent; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public BankType getBankType() { return bankType; }
    public void setBankType(BankType bankType) { this.bankType = bankType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<Account> getAccounts() { return accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }

    // Helper methods
    public boolean isInternational() {
        return isCorrespondent != null && isCorrespondent;
    }

    public String getDisplayName() {
        return String.format("%s (%s)", name, bicCode);
    }

    @Override
    public String toString() {
        return String.format("Bank{id=%d, name='%s', bicCode='%s', country='%s', active=%s}", 
                getId(), name, bicCode, countryCode, isActive);
    }

    /**
     * Bank Type enumeration
     */
    public enum BankType {
        CORRESPONDENT("Correspondent Bank"),
        INTERNAL("Internal Branch"),
        CENTRAL("Central Bank"),
        COMMERCIAL("Commercial Bank"),
        INVESTMENT("Investment Bank");

        private final String displayName;

        BankType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}





