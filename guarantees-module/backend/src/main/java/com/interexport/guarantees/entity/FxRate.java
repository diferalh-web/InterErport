package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.FxRateProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing foreign exchange rates.
 * Implements requirements from F16 - Currency Rate Loading (API or manual).
 */
@Entity
@Table(name = "fx_rates", 
       indexes = {
           @Index(name = "idx_fx_date_currencies", columnList = "effectiveDate,baseCurrency,targetCurrency", unique = true),
           @Index(name = "idx_fx_provider", columnList = "provider"),
           @Index(name = "idx_fx_effective_date", columnList = "effectiveDate")
       })
public class FxRate extends BaseEntity {

    /**
     * Base currency (ISO 4217)
     */
    @Column(name = "base_currency", nullable = false, length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Base currency must be a valid ISO 4217 code")
    private String baseCurrency;

    /**
     * Target currency (ISO 4217)
     */
    @Column(name = "target_currency", nullable = false, length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Target currency must be a valid ISO 4217 code")
    private String targetCurrency;

    /**
     * Exchange rate (1 base currency = rate * target currency)
     */
    @Column(name = "rate", nullable = false, precision = 15, scale = 8)
    @NotNull
    @DecimalMin(value = "0.00000001", message = "Exchange rate must be positive")
    private BigDecimal rate;

    /**
     * Buying rate (bank buys target currency)
     */
    @Column(name = "buying_rate", precision = 15, scale = 8)
    @DecimalMin(value = "0.00000001")
    private BigDecimal buyingRate;

    /**
     * Selling rate (bank sells target currency)
     */
    @Column(name = "selling_rate", precision = 15, scale = 8)
    @DecimalMin(value = "0.00000001")
    private BigDecimal sellingRate;

    /**
     * Average rate (midpoint between buying and selling)
     */
    @Column(name = "average_rate", precision = 15, scale = 8)
    @DecimalMin(value = "0.00000001")
    private BigDecimal averageRate;

    /**
     * Date when the rate becomes effective
     */
    @Column(name = "effective_date", nullable = false)
    @NotNull
    private LocalDate effectiveDate;

    /**
     * Date until the rate is valid (optional)
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /**
     * Source provider of the rate
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @NotNull
    private FxRateProvider provider;

    /**
     * External reference from the provider (if applicable)
     */
    @Column(name = "provider_reference", length = 100)
    @Size(max = 100)
    private String providerReference;

    /**
     * Whether this rate is currently active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Timestamp when the rate was retrieved/updated
     */
    @Column(name = "retrieved_at")
    private java.time.LocalDateTime retrievedAt;

    /**
     * Notes about the rate or manual entry reason
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    @Size(max = 500)
    private String notes;

    // Constructors
    public FxRate() {}

    public FxRate(String baseCurrency, String targetCurrency, BigDecimal rate, 
                 LocalDate effectiveDate, FxRateProvider provider) {
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.effectiveDate = effectiveDate;
        this.provider = provider;
        this.retrievedAt = java.time.LocalDateTime.now();
    }

    public FxRate(String baseCurrency, String targetCurrency, BigDecimal buyingRate, 
                 BigDecimal sellingRate, LocalDate effectiveDate, FxRateProvider provider) {
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.buyingRate = buyingRate;
        this.sellingRate = sellingRate;
        this.averageRate = buyingRate.add(sellingRate).divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP);
        this.rate = this.averageRate;
        this.effectiveDate = effectiveDate;
        this.provider = provider;
        this.retrievedAt = java.time.LocalDateTime.now();
    }

    // Business methods

    /**
     * Check if the rate is currently valid
     */
    public boolean isValid() {
        LocalDate today = LocalDate.now();
        return isActive && 
               !today.isBefore(effectiveDate) && 
               (expiryDate == null || !today.isAfter(expiryDate));
    }

    /**
     * Check if the rate has expired
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Calculate the spread between buying and selling rates
     */
    public BigDecimal getSpread() {
        if (buyingRate != null && sellingRate != null) {
            return sellingRate.subtract(buyingRate);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate the spread percentage
     */
    public BigDecimal getSpreadPercentage() {
        if (buyingRate != null && sellingRate != null && buyingRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal spread = getSpread();
            return spread.divide(buyingRate, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get the appropriate rate for a specific transaction type
     */
    public BigDecimal getRateForType(String transactionType) {
        switch (transactionType.toUpperCase()) {
            case "BUY":
            case "BUYING":
                return buyingRate != null ? buyingRate : rate;
            case "SELL":
            case "SELLING":
                return sellingRate != null ? sellingRate : rate;
            case "AVERAGE":
            case "MID":
                return averageRate != null ? averageRate : rate;
            default:
                return rate;
        }
    }

    /**
     * Convert amount from base currency to target currency
     */
    public BigDecimal convertFromBase(BigDecimal baseAmount, String transactionType) {
        if (baseAmount == null) return null;
        BigDecimal conversionRate = getRateForType(transactionType);
        return baseAmount.multiply(conversionRate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Convert amount from target currency to base currency
     */
    public BigDecimal convertToBase(BigDecimal targetAmount, String transactionType) {
        if (targetAmount == null) return null;
        BigDecimal conversionRate = getRateForType(transactionType);
        if (conversionRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot convert with zero exchange rate");
        }
        return targetAmount.divide(conversionRate, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Create the inverse rate (swap base and target currencies)
     */
    public FxRate createInverseRate() {
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot create inverse of zero rate");
        }
        
        FxRate inverseRate = new FxRate();
        inverseRate.setBaseCurrency(this.targetCurrency);
        inverseRate.setTargetCurrency(this.baseCurrency);
        inverseRate.setRate(BigDecimal.ONE.divide(this.rate, 8, java.math.RoundingMode.HALF_UP));
        
        if (this.buyingRate != null) {
            inverseRate.setSellingRate(BigDecimal.ONE.divide(this.buyingRate, 8, java.math.RoundingMode.HALF_UP));
        }
        if (this.sellingRate != null) {
            inverseRate.setBuyingRate(BigDecimal.ONE.divide(this.sellingRate, 8, java.math.RoundingMode.HALF_UP));
        }
        if (this.averageRate != null) {
            inverseRate.setAverageRate(BigDecimal.ONE.divide(this.averageRate, 8, java.math.RoundingMode.HALF_UP));
        }
        
        inverseRate.setEffectiveDate(this.effectiveDate);
        inverseRate.setExpiryDate(this.expiryDate);
        inverseRate.setProvider(this.provider);
        inverseRate.setIsActive(this.isActive);
        inverseRate.setRetrievedAt(java.time.LocalDateTime.now());
        
        return inverseRate;
    }

    // Getters and Setters

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getBuyingRate() {
        return buyingRate;
    }

    public void setBuyingRate(BigDecimal buyingRate) {
        this.buyingRate = buyingRate;
    }

    public BigDecimal getSellingRate() {
        return sellingRate;
    }

    public void setSellingRate(BigDecimal sellingRate) {
        this.sellingRate = sellingRate;
    }

    public BigDecimal getAverageRate() {
        return averageRate;
    }

    public void setAverageRate(BigDecimal averageRate) {
        this.averageRate = averageRate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public FxRateProvider getProvider() {
        return provider;
    }

    public void setProvider(FxRateProvider provider) {
        this.provider = provider;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public java.time.LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(java.time.LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
