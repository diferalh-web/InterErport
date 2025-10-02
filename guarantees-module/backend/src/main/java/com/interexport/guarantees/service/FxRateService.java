package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.FxRate;
import com.interexport.guarantees.entity.enums.FxRateProvider;
import com.interexport.guarantees.exception.FxRateNotFoundException;
import com.interexport.guarantees.repository.FxRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for foreign exchange rate management.
 * Implements requirements from F16 - Currency Rate Loading (API or manual).
 */
@Service
@Transactional
public class FxRateService {

    private final FxRateRepository fxRateRepository;

    @Value("${app.guarantees.fx-rate.base-currency:USD}")
    private String baseCurrency;

    @Value("${app.guarantees.fx-rate.default-provider:MANUAL}")
    private String defaultProvider;

    @Value("${app.guarantees.fx-rate.cache-duration-minutes:30}")
    private int cacheDurationMinutes;

    @Autowired
    public FxRateService(FxRateRepository fxRateRepository) {
        this.fxRateRepository = fxRateRepository;
    }

    /**
     * Get current exchange rate for currency pair
     * UC16.4: Fallback to manual rate if API fails
     * Uses Redis caching with 1-hour TTL for FX rates
     */
    @Cacheable(value = "fxRates", key = "#fromCurrency + '_' + #toCurrency", cacheManager = "cacheManager")
    @Transactional(readOnly = true)
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        
        // Try direct rate first
        Optional<FxRate> directRate = fxRateRepository.findCurrentRate(fromCurrency, toCurrency);
        if (directRate.isPresent()) {
            return directRate.get().getRate();
        }
        
        // Try inverse rate
        Optional<FxRate> inverseRate = fxRateRepository.findCurrentRate(toCurrency, fromCurrency);
        if (inverseRate.isPresent()) {
            BigDecimal rate = inverseRate.get().getRate();
            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.ONE.divide(rate, 8, java.math.RoundingMode.HALF_UP);
            }
        }
        
        // Try cross-rate through base currency (USD)
        if (!fromCurrency.equals(baseCurrency) && !toCurrency.equals(baseCurrency)) {
            try {
                BigDecimal fromToBase = getExchangeRate(fromCurrency, baseCurrency);
                BigDecimal baseToTarget = getExchangeRate(baseCurrency, toCurrency);
                return fromToBase.multiply(baseToTarget);
            } catch (FxRateNotFoundException e) {
                // Continue to throw original exception
            }
        }
        
        throw new FxRateNotFoundException(
            String.format("Exchange rate not found for %s to %s", fromCurrency, toCurrency));
    }

    /**
     * Get exchange rate for specific date
     */
    @Transactional(readOnly = true)
    public BigDecimal getExchangeRateForDate(String fromCurrency, String toCurrency, LocalDate date) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        
        Optional<FxRate> rate = fxRateRepository.findRateForDate(fromCurrency, toCurrency, date);
        if (rate.isPresent()) {
            return rate.get().getRate();
        }
        
        // Try inverse rate
        Optional<FxRate> inverseRate = fxRateRepository.findRateForDate(toCurrency, fromCurrency, date);
        if (inverseRate.isPresent()) {
            BigDecimal rateValue = inverseRate.get().getRate();
            if (rateValue.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.ONE.divide(rateValue, 8, java.math.RoundingMode.HALF_UP);
            }
        }
        
        throw new FxRateNotFoundException(
            String.format("Exchange rate not found for %s to %s on %s", fromCurrency, toCurrency, date));
    }

    /**
     * Get selling rate (bank sells foreign currency)
     * UC3.5: Select selling FX rate for charged commissions
     */
    @Transactional(readOnly = true)
    public BigDecimal getSellingRate(String fromCurrency, String toCurrency) {
        Optional<FxRate> rate = fxRateRepository.findCurrentRate(fromCurrency, toCurrency);
        if (rate.isPresent()) {
            return rate.get().getRateForType("SELLING");
        }
        
        throw new FxRateNotFoundException(
            String.format("Selling rate not found for %s to %s", fromCurrency, toCurrency));
    }

    /**
     * Get average rate for liability calculation
     * UC3.4: Select average FX rate for liability
     */
    @Transactional(readOnly = true)
    public BigDecimal getAverageRate(String fromCurrency, String toCurrency) {
        Optional<FxRate> rate = fxRateRepository.findCurrentRate(fromCurrency, toCurrency);
        if (rate.isPresent()) {
            return rate.get().getRateForType("AVERAGE");
        }
        
        throw new FxRateNotFoundException(
            String.format("Average rate not found for %s to %s", fromCurrency, toCurrency));
    }

    /**
     * Create or update manual exchange rate
     * UC16.3: Register manual rate by admin with effective date
     */
    public FxRate createManualRate(String baseCurrency, String targetCurrency, 
                                  BigDecimal rate, LocalDate effectiveDate, 
                                  BigDecimal buyingRate, BigDecimal sellingRate,
                                  String notes) {
        
        // Validate input
        validateRateCreation(baseCurrency, targetCurrency, rate, effectiveDate);
        
        // Check for existing rate on same date
        List<FxRate> duplicates = fxRateRepository.findDuplicateRates(
            baseCurrency, targetCurrency, effectiveDate, -1L);
        
        if (!duplicates.isEmpty()) {
            // Deactivate existing rate
            duplicates.forEach(existing -> existing.setIsActive(false));
            fxRateRepository.saveAll(duplicates);
        }
        
        // Create new rate
        FxRate fxRate = new FxRate();
        fxRate.setBaseCurrency(baseCurrency);
        fxRate.setTargetCurrency(targetCurrency);
        fxRate.setRate(rate);
        fxRate.setEffectiveDate(effectiveDate);
        fxRate.setProvider(FxRateProvider.MANUAL);
        fxRate.setIsActive(true);
        fxRate.setRetrievedAt(LocalDateTime.now());
        fxRate.setNotes(notes);
        
        if (buyingRate != null && sellingRate != null) {
            fxRate.setBuyingRate(buyingRate);
            fxRate.setSellingRate(sellingRate);
            fxRate.setAverageRate(buyingRate.add(sellingRate)
                .divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP));
        }
        
        return fxRateRepository.save(fxRate);
    }

    /**
     * Fetch rates from external provider (placeholder for API integration)
     * UC16.2: Fetch daily rates via provider API
     */
    public List<FxRate> fetchRatesFromProvider(FxRateProvider provider) {
        // This would integrate with actual APIs like ECB, Bloomberg, etc.
        // For POC, we'll create sample rates
        
        switch (provider) {
            case ECB:
                return fetchFromEcb();
            case BLOOMBERG:
                return fetchFromBloomberg();
            default:
                throw new IllegalArgumentException("API provider not supported: " + provider);
        }
    }

    /**
     * Get all current active rates
     */
    @Transactional(readOnly = true)
    public List<FxRate> getAllCurrentActiveRates() {
        return fxRateRepository.findAllCurrentActiveRates();
    }

    /**
     * Get rates with pagination
     */
    @Transactional(readOnly = true)
    public Page<FxRate> findAll(Pageable pageable) {
        return fxRateRepository.findAll(pageable);
    }

    /**
     * Get rates by provider
     */
    @Transactional(readOnly = true)
    public List<FxRate> findByProvider(FxRateProvider provider) {
        return fxRateRepository.findByProvider(provider);
    }

    /**
     * Refresh rates that are older than cache duration
     */
    public void refreshStaleRates() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(cacheDurationMinutes);
        List<FxRate> staleRates = fxRateRepository.findRatesNeedingRefresh(threshold);
        
        for (FxRate rate : staleRates) {
            if (rate.getProvider().requiresApi()) {
                try {
                    // Refresh from API
                    refreshRateFromApi(rate);
                } catch (Exception e) {
                    // Log error but continue with other rates
                    System.err.println("Failed to refresh rate " + rate.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get distinct currency pairs
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDistinctCurrencyPairs() {
        return fxRateRepository.findDistinctCurrencyPairs();
    }

    /**
     * Convert amount between currencies
     */
    @Transactional(readOnly = true)
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) return null;
        if (fromCurrency.equals(toCurrency)) return amount;
        
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Convert amount with detailed result
     */
    @Transactional(readOnly = true)
    public ConversionResult convertAmountWithDetails(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) return null;
        
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        BigDecimal convertedAmount;
        
        if (fromCurrency.equals(toCurrency)) {
            convertedAmount = amount;
            rate = BigDecimal.ONE;
        } else {
            convertedAmount = amount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        
        return new ConversionResult(amount, fromCurrency, toCurrency, convertedAmount, rate);
    }

    /**
     * Get rates for specific effective date
     */
    @Transactional(readOnly = true)
    public List<FxRate> findByEffectiveDate(LocalDate date) {
        return fxRateRepository.findByEffectiveDate(date);
    }

    /**
     * Create a new FX rate
     */
    @Transactional
    public FxRate createRate(FxRate fxRate) {
        validateRateCreation(fxRate.getBaseCurrency(), fxRate.getTargetCurrency(),
                           fxRate.getRate(), fxRate.getEffectiveDate());
        
        // Set default values
        if (fxRate.getRetrievedAt() == null) {
            fxRate.setRetrievedAt(LocalDateTime.now());
        }
        if (fxRate.getIsActive() == null) {
            fxRate.setIsActive(true);
        }
        
        return fxRateRepository.save(fxRate);
    }

    /**
     * Update an existing FX rate
     */
    @Transactional
    public FxRate updateRate(Long id, FxRate updatedRate) {
        FxRate existingRate = findById(id);
        
        // Update fields
        if (updatedRate.getRate() != null) {
            existingRate.setRate(updatedRate.getRate());
        }
        if (updatedRate.getEffectiveDate() != null) {
            existingRate.setEffectiveDate(updatedRate.getEffectiveDate());
        }
        if (updatedRate.getProvider() != null) {
            existingRate.setProvider(updatedRate.getProvider());
        }
        if (updatedRate.getIsActive() != null) {
            existingRate.setIsActive(updatedRate.getIsActive());
        }
        
        existingRate.setRetrievedAt(LocalDateTime.now());
        
        return fxRateRepository.save(existingRate);
    }

    /**
     * Find FX rate by ID
     */
    @Transactional(readOnly = true)
    public FxRate findById(Long id) {
        return fxRateRepository.findById(id)
                .orElseThrow(() -> new FxRateNotFoundException("FX rate not found with ID: " + id));
    }

    /**
     * Get all supported currencies
     */
    @Transactional(readOnly = true)
    public List<String> getAllSupportedCurrencies() {
        return fxRateRepository.findAllSupportedCurrencies();
    }

    /**
     * Get latest rate for currency pair (returns Optional<FxRate>)
     */
    @Transactional(readOnly = true)
    public Optional<FxRate> getLatestRate(String baseCurrency, String targetCurrency) {
        return fxRateRepository.findCurrentRate(baseCurrency, targetCurrency);
    }

    // Private helper methods
    
    private void validateRateCreation(String baseCurrency, String targetCurrency, 
                                    BigDecimal rate, LocalDate effectiveDate) {
        if (baseCurrency == null || baseCurrency.trim().isEmpty()) {
            throw new IllegalArgumentException("Base currency is required");
        }
        if (targetCurrency == null || targetCurrency.trim().isEmpty()) {
            throw new IllegalArgumentException("Target currency is required");
        }
        if (baseCurrency.equals(targetCurrency)) {
            throw new IllegalArgumentException("Base and target currencies cannot be the same");
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("Effective date is required");
        }
    }

    private void refreshRateFromApi(FxRate rate) {
        // Placeholder for actual API integration
        rate.setRetrievedAt(LocalDateTime.now());
        fxRateRepository.save(rate);
    }

    private List<FxRate> fetchFromEcb() {
        // Placeholder for ECB API integration
        // In real implementation, this would fetch from ECB's API
        return List.of();
    }

    private List<FxRate> fetchFromBloomberg() {
        // Placeholder for Bloomberg API integration
        // In real implementation, this would fetch from Bloomberg's API
        return List.of();
    }

    /**
     * Result of currency conversion
     */
    public static class ConversionResult {
        private final BigDecimal originalAmount;
        private final String fromCurrency;
        private final String toCurrency;
        private final BigDecimal convertedAmount;
        private final BigDecimal exchangeRate;
        private final LocalDateTime timestamp;

        public ConversionResult(BigDecimal originalAmount, String fromCurrency, String toCurrency, 
                              BigDecimal convertedAmount, BigDecimal exchangeRate) {
            this.originalAmount = originalAmount;
            this.fromCurrency = fromCurrency;
            this.toCurrency = toCurrency;
            this.convertedAmount = convertedAmount;
            this.exchangeRate = exchangeRate;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public BigDecimal getOriginalAmount() { return originalAmount; }
        public String getFromCurrency() { return fromCurrency; }
        public String getToCurrency() { return toCurrency; }
        public BigDecimal getConvertedAmount() { return convertedAmount; }
        public BigDecimal getExchangeRate() { return exchangeRate; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
