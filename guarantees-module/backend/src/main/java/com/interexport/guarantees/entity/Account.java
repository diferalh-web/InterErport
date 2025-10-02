package com.interexport.guarantees.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Account entity for Parameters Module (F10)
 * Represents bank accounts used for guarantee processing and GL mapping
 * 
 * Business Rules:
 * - Each account must be associated with a bank
 * - Account numbers must be unique within a bank
 * - Active accounts can be used for new transactions
 * - GL codes map to accounting system
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_number", columnList = "accountNumber"),
    @Index(name = "idx_account_currency", columnList = "currency"),
    @Index(name = "idx_account_gl_code", columnList = "glCode"),
    @Index(name = "idx_account_bank", columnList = "bank_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_bank_account", columnNames = {"bank_id", "accountNumber"})
})
public class Account extends BaseEntity {

    @NotBlank(message = "Account number is required")
    @Size(min = 5, max = 50, message = "Account number must be between 5 and 50 characters")
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @NotBlank(message = "Account name is required")
    @Size(min = 2, max = 200, message = "Account name must be between 2 and 200 characters")
    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType = AccountType.LIABILITY;

    @Size(max = 20, message = "GL code cannot exceed 20 characters")
    @Column(name = "gl_code", length = 20)
    private String glCode;

    @Size(max = 200, message = "GL description cannot exceed 200 characters")
    @Column(name = "gl_description", length = 200)
    private String glDescription;

    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    @NotNull(message = "Bank is required")
    @JsonBackReference
    private Bank bank;

    // Constructors
    public Account() {}

    public Account(String accountNumber, String accountName, String currency, Bank bank) {
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.currency = currency;
        this.bank = bank;
    }

    // Getters and Setters
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency != null ? currency.toUpperCase() : null; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getGlCode() { return glCode; }
    public void setGlCode(String glCode) { this.glCode = glCode; }

    public String getGlDescription() { return glDescription; }
    public void setGlDescription(String glDescription) { this.glDescription = glDescription; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Bank getBank() { return bank; }
    public void setBank(Bank bank) { this.bank = bank; }

    // Helper methods
    public String getFullAccountNumber() {
        return bank != null ? String.format("%s-%s", bank.getBicCode(), accountNumber) : accountNumber;
    }

    public BigDecimal getAvailableBalance() {
        return balance.add(creditLimit);
    }

    public String getDisplayName() {
        return String.format("%s (%s %s)", accountName, accountNumber, currency);
    }

    @Override
    public String toString() {
        return String.format("Account{id=%d, number='%s', name='%s', currency='%s', type=%s, active=%s}", 
                getId(), accountNumber, accountName, currency, accountType, isActive);
    }

    /**
     * Account Type enumeration for GL mapping
     */
    public enum AccountType {
        LIABILITY("Liability Account", "L"),
        ASSET("Asset Account", "A"),
        INCOME("Income Account", "I"),
        EXPENSE("Expense Account", "E"),
        EQUITY("Equity Account", "Q"),
        NOSTRO("Nostro Account", "N"),
        VOSTRO("Vostro Account", "V"),
        SUSPENSE("Suspense Account", "S");

        private final String displayName;
        private final String code;

        AccountType(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }

        public String getDisplayName() { return displayName; }
        public String getCode() { return code; }
    }
}





