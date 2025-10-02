package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.CommissionParameter;
import com.interexport.guarantees.entity.FeeItem;
import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.repository.CommissionParameterRepository;
import com.interexport.guarantees.repository.FeeItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for commission and fee calculations.
 * Implements requirements from F3 - Commission and Exchange Rate Calculation.
 */
@Service
@Transactional
public class CommissionCalculationService {

    private final FeeItemRepository feeItemRepository;
    private final FxRateService fxRateService;
    private final CommissionParameterRepository commissionParameterRepository;

    @Value("${app.guarantees.commission.default-installments:4}")
    private int defaultInstallments;

    @Value("${app.guarantees.commission.minimum-commission:50.00}")
    private BigDecimal defaultMinimumCommission;

    @Value("${app.guarantees.commission.rounding-scale:2}")
    private int roundingScale;

    // Default commission rates by guarantee type (in percentage) - fallback values
    private static final BigDecimal PERFORMANCE_COMMISSION_RATE = new BigDecimal("1.5");
    private static final BigDecimal ADVANCE_PAYMENT_COMMISSION_RATE = new BigDecimal("2.0");
    private static final BigDecimal BID_BOND_COMMISSION_RATE = new BigDecimal("1.0");
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("1.5");

    @Autowired
    public CommissionCalculationService(FeeItemRepository feeItemRepository, 
                                      FxRateService fxRateService,
                                      CommissionParameterRepository commissionParameterRepository) {
        this.feeItemRepository = feeItemRepository;
        this.fxRateService = fxRateService;
        this.commissionParameterRepository = commissionParameterRepository;
    }

    /**
     * Calculate and create commission fees for a guarantee
     * ENHANCED: UC3.1-UC3.6 with advanced multi-currency and installment logic
     */
    public List<FeeItem> calculateCommissionFees(GuaranteeContract guarantee) {
        // Get commission parameters from configuration
        CommissionParameter commissionParams = getCommissionParameterForGuarantee(guarantee);
        
        if (commissionParams == null) {
            throw new IllegalStateException(String.format(
                "No commission parameters found for guarantee type %s, currency %s, domestic %s",
                guarantee.getGuaranteeType(), guarantee.getCurrency(), guarantee.getIsDomestic()));
        }
        
        // Calculate base commission using configuration
        BigDecimal calculatedCommission = calculateAdvancedCommission(guarantee, commissionParams);
        
        // Apply currency conversion using appropriate rates
        CurrencyConversionResult conversionResult = convertCommissionToBaseCurrency(
            calculatedCommission, guarantee.getCurrency());
        
        // Create installment fees based on configuration
        List<FeeItem> feeItems = createAdvancedInstallmentFees(
            guarantee, commissionParams, calculatedCommission, conversionResult);
        
        return feeItemRepository.saveAll(feeItems);
    }

    /**
     * Calculate commission with advanced business rules and multi-currency support
     * UC3.1: Calculate base commission by amount
     * UC3.2: Apply minimum and maximum when applicable
     * UC3.6: Multi-currency commission calculation with current exchange rates
     */
    public BigDecimal calculateAdvancedCommission(GuaranteeContract guarantee, CommissionParameter params) {
        if (guarantee == null || params == null) {
            throw new IllegalArgumentException("Guarantee and commission parameters cannot be null");
        }
        
        BigDecimal guaranteeAmount = guarantee.getAmount();
        BigDecimal commissionRate = params.getCommissionRate();
        
        // Calculate base commission based on calculation basis
        BigDecimal baseCommission;
        switch (params.getCalculationBasis()) {
            case FULL_AMOUNT:
                baseCommission = calculatePercentageCommission(guaranteeAmount, commissionRate);
                break;
            case UTILIZED_AMOUNT:
                // For POC, same as full amount - in production would check utilization
                baseCommission = calculatePercentageCommission(guaranteeAmount, commissionRate);
                break;
            case OUTSTANDING_AMOUNT:
                // For POC, same as full amount - in production would check outstanding balance
                baseCommission = calculatePercentageCommission(guaranteeAmount, commissionRate);
                break;
            case TIME_BASED:
                baseCommission = calculateTimeBasedCommission(guarantee, commissionRate);
                break;
            default:
                baseCommission = calculatePercentageCommission(guaranteeAmount, commissionRate);
        }
        
        // Apply minimum commission if specified
        if (params.getMinimumAmount() != null && baseCommission.compareTo(params.getMinimumAmount()) < 0) {
            baseCommission = params.getMinimumAmount();
        }
        
        // Apply maximum commission if specified
        if (params.getMaximumAmount() != null && baseCommission.compareTo(params.getMaximumAmount()) > 0) {
            baseCommission = params.getMaximumAmount();
        }
        
        return baseCommission;
    }

    /**
     * Convert commission to base currency using appropriate FX rates
     * UC3.4: Select average FX rate for liability
     * UC3.5: Select selling FX rate for charged commissions
     */
    private CurrencyConversionResult convertCommissionToBaseCurrency(BigDecimal amount, String fromCurrency) {
        if ("USD".equals(fromCurrency)) {
            return new CurrencyConversionResult(amount, fromCurrency, "USD", amount, BigDecimal.ONE, "NONE");
        }
        
        try {
            // Use selling rate for commission charges (bank perspective)
            BigDecimal sellingRate = fxRateService.getSellingRate(fromCurrency, "USD");
            BigDecimal convertedAmount = amount.multiply(sellingRate)
                .setScale(roundingScale, RoundingMode.HALF_UP);
            
            return new CurrencyConversionResult(amount, fromCurrency, "USD", convertedAmount, sellingRate, "SELLING");
            
        } catch (Exception e) {
            // Fallback to average rate if selling rate not available
            try {
                BigDecimal averageRate = fxRateService.getAverageRate(fromCurrency, "USD");
                BigDecimal convertedAmount = amount.multiply(averageRate)
                    .setScale(roundingScale, RoundingMode.HALF_UP);
                
                return new CurrencyConversionResult(amount, fromCurrency, "USD", convertedAmount, averageRate, "AVERAGE");
                
            } catch (Exception fallbackError) {
                // Final fallback to standard rate
                BigDecimal standardRate = fxRateService.getExchangeRate(fromCurrency, "USD");
                BigDecimal convertedAmount = amount.multiply(standardRate)
                    .setScale(roundingScale, RoundingMode.HALF_UP);
                
                return new CurrencyConversionResult(amount, fromCurrency, "USD", convertedAmount, standardRate, "STANDARD");
            }
        }
    }

    /**
     * Create advanced installment fees with flexible distribution
     * UC3.3: Defer commission in N installments with manual override capability
     */
    private List<FeeItem> createAdvancedInstallmentFees(GuaranteeContract guarantee, 
                                                        CommissionParameter params,
                                                        BigDecimal totalCommission,
                                                        CurrencyConversionResult conversionResult) {
        List<FeeItem> feeItems = new ArrayList<>();
        
        int installments = params.getDefaultInstallments();
        LocalDate startDate = guarantee.getIssueDate() != null ? guarantee.getIssueDate() : LocalDate.now();
        
        if (installments <= 1) {
            // Single payment
            FeeItem singleFee = createAdvancedFeeItem(guarantee, totalCommission, conversionResult, 
                startDate, "Commission (Single Payment)", 1, 1);
            feeItems.add(singleFee);
        } else {
            // Multiple installments with equal distribution
            BigDecimal installmentAmount = totalCommission.divide(
                BigDecimal.valueOf(installments), roundingScale, RoundingMode.HALF_UP);
            
            // Handle rounding differences in last installment
            BigDecimal runningTotal = BigDecimal.ZERO;
            
            for (int i = 1; i <= installments; i++) {
                BigDecimal currentInstallmentAmount;
                LocalDate dueDate = startDate.plusMonths(i);
                
                if (i == installments) {
                    // Last installment gets the remainder to ensure exact total
                    currentInstallmentAmount = totalCommission.subtract(runningTotal);
                } else {
                    currentInstallmentAmount = installmentAmount;
                    runningTotal = runningTotal.add(installmentAmount);
                }
                
                FeeItem installmentFee = createAdvancedFeeItem(guarantee, currentInstallmentAmount, 
                    conversionResult, dueDate, 
                    String.format("Commission Installment %d/%d", i, installments), i, installments);
                feeItems.add(installmentFee);
            }
        }
        
        return feeItems;
    }

    /**
     * Calculate base commission amount
     * UC3.1 & UC3.2: Calculate base commission by amount and apply minimum
     */
    public BigDecimal calculateBaseCommission(BigDecimal guaranteeAmount, 
                                            BigDecimal commissionRate, 
                                            BigDecimal minimumCommission) {
        if (guaranteeAmount == null || commissionRate == null) {
            throw new IllegalArgumentException("Guarantee amount and commission rate cannot be null");
        }
        
        // Calculate percentage-based commission with banking rounding
        BigDecimal calculatedCommission = guaranteeAmount
            .multiply(commissionRate)
            .divide(BigDecimal.valueOf(100), roundingScale, RoundingMode.HALF_UP);
        
        // Apply minimum commission if specified and calculated amount is lower
        if (minimumCommission != null && calculatedCommission.compareTo(minimumCommission) < 0) {
            calculatedCommission = minimumCommission;
        }
        
        return calculatedCommission;
    }

    /**
     * Create installment fees for deferred payment
     * UC3.3: Defer commission in N installments by default per product rule
     */
    public List<FeeItem> createInstallmentFees(GuaranteeContract guarantee, 
                                             BigDecimal totalCommission, 
                                             BigDecimal totalCommissionInBase) {
        List<FeeItem> installments = new ArrayList<>();
        
        // Calculate installment amount (distribute evenly)
        BigDecimal installmentAmount = totalCommission.divide(
            BigDecimal.valueOf(defaultInstallments), roundingScale, RoundingMode.HALF_UP);
        
        BigDecimal installmentAmountBase = totalCommissionInBase.divide(
            BigDecimal.valueOf(defaultInstallments), roundingScale, RoundingMode.HALF_UP);
        
        // Handle rounding differences by adjusting the last installment
        BigDecimal remainingAmount = totalCommission;
        BigDecimal remainingAmountBase = totalCommissionInBase;
        
        for (int i = 1; i <= defaultInstallments; i++) {
            BigDecimal thisInstallmentAmount;
            BigDecimal thisInstallmentAmountBase;
            
            if (i == defaultInstallments) {
                // Last installment gets any remaining amount due to rounding
                thisInstallmentAmount = remainingAmount;
                thisInstallmentAmountBase = remainingAmountBase;
            } else {
                thisInstallmentAmount = installmentAmount;
                thisInstallmentAmountBase = installmentAmountBase;
                remainingAmount = remainingAmount.subtract(installmentAmount);
                remainingAmountBase = remainingAmountBase.subtract(installmentAmountBase);
            }
            
            // Calculate due date (quarterly payments)
            LocalDate dueDate = guarantee.getIssueDate().plusMonths(i * 3L);
            
            FeeItem installment = FeeItem.createInstallment(
                createBaseFeeItem(guarantee, totalCommission),
                i, defaultInstallments, thisInstallmentAmount, dueDate);
            
            installment.setBaseAmount(thisInstallmentAmountBase);
            installment.setExchangeRate(guarantee.getExchangeRate());
            
            installments.add(installment);
        }
        
        return installments;
    }

    /**
     * Manual distribution of commission amounts
     * UC3.4: Ability for authorized user to manually distribute amounts
     */
    public List<FeeItem> createManualInstallments(GuaranteeContract guarantee,
                                                 List<BigDecimal> installmentAmounts,
                                                 List<LocalDate> dueDates) {
        if (installmentAmounts.size() != dueDates.size()) {
            throw new IllegalArgumentException("Installment amounts and due dates must have same size");
        }
        
        // Validate total equals original commission
        BigDecimal totalManualAmount = installmentAmounts.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal originalCommission = calculateTotalCommissionForGuarantee(guarantee);
        
        if (totalManualAmount.compareTo(originalCommission) != 0) {
            throw new IllegalArgumentException(
                String.format("Manual installment total (%s) must equal calculated commission (%s)", 
                             totalManualAmount, originalCommission));
        }
        
        List<FeeItem> manualInstallments = new ArrayList<>();
        
        for (int i = 0; i < installmentAmounts.size(); i++) {
            BigDecimal amount = installmentAmounts.get(i);
            LocalDate dueDate = dueDates.get(i);
            
            BigDecimal baseAmount = convertToBaseCurrency(amount, guarantee.getCurrency());
            
            FeeItem installment = FeeItem.createInstallment(
                createBaseFeeItem(guarantee, originalCommission),
                i + 1, installmentAmounts.size(), amount, dueDate);
            
            installment.setBaseAmount(baseAmount);
            installment.setExchangeRate(guarantee.getExchangeRate());
            installment.setIsCalculated(false); // Manual entry
            installment.setNotes("Manually distributed installment");
            
            manualInstallments.add(installment);
        }
        
        return feeItemRepository.saveAll(manualInstallments);
    }

    /**
     * Convert amount to base currency using current exchange rate
     * UC3.6: If guarantee is in different currency, use current day's exchange rate
     */
    public BigDecimal convertToBaseCurrency(BigDecimal amount, String fromCurrency) {
        if ("USD".equals(fromCurrency)) {
            return amount;
        }
        
        try {
            BigDecimal exchangeRate = fxRateService.getExchangeRate(fromCurrency, "USD");
            return amount.multiply(exchangeRate).setScale(roundingScale, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to get exchange rate for %s to USD: %s", 
                             fromCurrency, e.getMessage()), e);
        }
    }

    /**
     * Get commission rate based on guarantee type and client configuration
     */
    public BigDecimal getCommissionRateForGuarantee(GuaranteeContract guarantee) {
        // In a real implementation, this would check client-specific rates,
        // product configurations, etc.
        switch (guarantee.getGuaranteeType()) {
            case PERFORMANCE:
                return PERFORMANCE_COMMISSION_RATE;
            case ADVANCE_PAYMENT:
                return ADVANCE_PAYMENT_COMMISSION_RATE;
            case BID_BOND:
                return BID_BOND_COMMISSION_RATE;
            default:
                return DEFAULT_COMMISSION_RATE;
        }
    }

    /**
     * Get minimum commission for guarantee based on configuration
     */
    public BigDecimal getMinimumCommissionForGuarantee(GuaranteeContract guarantee) {
        // In a real implementation, this could vary by client, currency, etc.
        return defaultMinimumCommission;
    }

    /**
     * Calculate total commission for a guarantee (before installments)
     */
    public BigDecimal calculateTotalCommissionForGuarantee(GuaranteeContract guarantee) {
        BigDecimal commissionRate = getCommissionRateForGuarantee(guarantee);
        BigDecimal minimumCommission = getMinimumCommissionForGuarantee(guarantee);
        
        return calculateBaseCommission(guarantee.getAmount(), commissionRate, minimumCommission);
    }

    /**
     * Get total unpaid fees for a guarantee
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalUnpaidFees(Long guaranteeId) {
        BigDecimal total = feeItemRepository.getTotalUnpaidFeesForGuarantee(guaranteeId);
        return total != null ? total : BigDecimal.ZERO;
    }

    // Private helper methods
    
    private FeeItem createBaseFeeItem(GuaranteeContract guarantee, BigDecimal totalAmount) {
        FeeItem baseFeeItem = new FeeItem();
        baseFeeItem.setGuarantee(guarantee);
        baseFeeItem.setFeeType("COMMISSION");
        baseFeeItem.setDescription("Guarantee Commission");
        baseFeeItem.setAmount(totalAmount);
        baseFeeItem.setCurrency(guarantee.getCurrency());
        baseFeeItem.setIsCalculated(true);
        return baseFeeItem;
    }

    /**
     * Get commission parameter configuration for a guarantee
     */
    private CommissionParameter getCommissionParameterForGuarantee(GuaranteeContract guarantee) {
        Optional<CommissionParameter> parameter = commissionParameterRepository
            .findActiveParameterForCriteria(
                guarantee.getGuaranteeType(),
                guarantee.getCurrency(),
                guarantee.getIsDomestic(),
                "STANDARD", // Default client segment
                LocalDate.now()
            );
        
        return parameter.orElse(null);
    }

    /**
     * Calculate percentage-based commission with banking rounding
     */
    private BigDecimal calculatePercentageCommission(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(roundingScale, RoundingMode.HALF_UP);
    }

    /**
     * Calculate time-based commission (for bonds with duration consideration)
     */
    private BigDecimal calculateTimeBasedCommission(GuaranteeContract guarantee, BigDecimal baseRate) {
        if (guarantee.getIssueDate() == null || guarantee.getExpiryDate() == null) {
            // Fallback to standard percentage calculation
            return calculatePercentageCommission(guarantee.getAmount(), baseRate);
        }
        
        // Calculate duration in months
        long months = ChronoUnit.MONTHS.between(guarantee.getIssueDate(), guarantee.getExpiryDate());
        if (months <= 0) months = 1; // Minimum 1 month
        
        // Apply time factor (longer duration = higher commission)
        BigDecimal timeFactor = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal adjustedRate = baseRate.multiply(timeFactor);
        
        return calculatePercentageCommission(guarantee.getAmount(), adjustedRate);
    }

    /**
     * Create advanced fee item with detailed metadata
     */
    private FeeItem createAdvancedFeeItem(GuaranteeContract guarantee, BigDecimal amount, 
                                        CurrencyConversionResult conversion, LocalDate dueDate, 
                                        String description, int installmentNumber, int totalInstallments) {
        FeeItem feeItem = new FeeItem();
        feeItem.setGuarantee(guarantee);
        feeItem.setFeeType("COMMISSION");
        feeItem.setDescription(description);
        feeItem.setAmount(amount);
        feeItem.setCurrency(guarantee.getCurrency());
        feeItem.setDueDate(dueDate);
        feeItem.setIsCalculated(true);
        
        // Add conversion details as notes if currency was converted
        if (!conversion.getRateType().equals("NONE")) {
            String notes = String.format("Converted to USD: %s (Rate: %s %s, Type: %s)", 
                conversion.getConvertedAmount(), 
                conversion.getExchangeRate(), 
                conversion.getFromCurrency() + "/" + conversion.getToCurrency(),
                conversion.getRateType());
            feeItem.setNotes(notes);
        }
        
        return feeItem;
    }

    /**
     * Result class for currency conversion operations
     */
    public static class CurrencyConversionResult {
        private final BigDecimal originalAmount;
        private final String fromCurrency;
        private final String toCurrency;
        private final BigDecimal convertedAmount;
        private final BigDecimal exchangeRate;
        private final String rateType; // SELLING, AVERAGE, STANDARD, NONE
        private final LocalDateTime timestamp;

        public CurrencyConversionResult(BigDecimal originalAmount, String fromCurrency, String toCurrency, 
                                      BigDecimal convertedAmount, BigDecimal exchangeRate, String rateType) {
            this.originalAmount = originalAmount;
            this.fromCurrency = fromCurrency;
            this.toCurrency = toCurrency;
            this.convertedAmount = convertedAmount;
            this.exchangeRate = exchangeRate;
            this.rateType = rateType;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public BigDecimal getOriginalAmount() { return originalAmount; }
        public String getFromCurrency() { return fromCurrency; }
        public String getToCurrency() { return toCurrency; }
        public BigDecimal getConvertedAmount() { return convertedAmount; }
        public BigDecimal getExchangeRate() { return exchangeRate; }
        public String getRateType() { return rateType; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("CurrencyConversion{%s %s -> %s %s @ %s (%s)}", 
                originalAmount, fromCurrency, convertedAmount, toCurrency, exchangeRate, rateType);
        }
    }

    /**
     * Manual installment distribution (for authorized users)
     * UC3.3: Ability for authorized user to manually distribute amounts
     */
    public List<FeeItem> createManualInstallmentDistribution(GuaranteeContract guarantee, 
                                                           List<ManualInstallmentRequest> distributions) {
        // Validate total equals commission amount
        BigDecimal totalDistributed = distributions.stream()
            .map(ManualInstallmentRequest::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expectedTotal = calculateTotalCommissionForGuarantee(guarantee);
        
        if (totalDistributed.compareTo(expectedTotal) != 0) {
            throw new IllegalArgumentException(String.format(
                "Manual distribution total (%s) does not match calculated commission (%s). " +
                "Please ensure the sum of all installments equals the total commission amount.",
                totalDistributed, expectedTotal));
        }
        
        // Create fee items from manual distribution
        List<FeeItem> feeItems = new ArrayList<>();
        CurrencyConversionResult conversion = convertCommissionToBaseCurrency(BigDecimal.ZERO, guarantee.getCurrency());
        
        for (int i = 0; i < distributions.size(); i++) {
            ManualInstallmentRequest distribution = distributions.get(i);
            
            FeeItem feeItem = createAdvancedFeeItem(guarantee, distribution.getAmount(), conversion,
                distribution.getDueDate(), 
                String.format("Commission Manual Installment %d/%d", i + 1, distributions.size()),
                i + 1, distributions.size());
            
            if (distribution.getNotes() != null) {
                String existingNotes = feeItem.getNotes() != null ? feeItem.getNotes() + "; " : "";
                feeItem.setNotes(existingNotes + distribution.getNotes());
            }
            
            feeItems.add(feeItem);
        }
        
        return feeItemRepository.saveAll(feeItems);
    }

    /**
     * Request class for manual installment distribution
     */
    public static class ManualInstallmentRequest {
        private BigDecimal amount;
        private LocalDate dueDate;
        private String notes;

        public ManualInstallmentRequest() {}

        public ManualInstallmentRequest(BigDecimal amount, LocalDate dueDate, String notes) {
            this.amount = amount;
            this.dueDate = dueDate;
            this.notes = notes;
        }

        // Getters and Setters
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
