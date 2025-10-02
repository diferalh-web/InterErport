package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.FeeItem;
import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.repository.FeeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommissionCalculationService.
 * Implements test requirements from F3 - Commission and Exchange Rate Calculation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Commission Calculation Service Tests")
class CommissionCalculationServiceTest {

    @Mock
    private FeeItemRepository feeItemRepository;

    @Mock
    private FxRateService fxRateService;

    @InjectMocks
    private CommissionCalculationService commissionService;

    private GuaranteeContract testGuarantee;

    @BeforeEach
    void setUp() {
        // Set configuration properties using reflection
        ReflectionTestUtils.setField(commissionService, "defaultInstallments", 4);
        ReflectionTestUtils.setField(commissionService, "defaultMinimumCommission", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(commissionService, "roundingScale", 2);

        testGuarantee = new GuaranteeContract(
                "GT-20231001-000001",
                GuaranteeType.PERFORMANCE,
                new BigDecimal("100000.00"),
                "USD",
                LocalDate.now(),
                LocalDate.now().plusMonths(12),
                1L,
                "Test Beneficiary"
        );
        testGuarantee.setId(1L);
    }

    @Test
    @DisplayName("T3.1: Deterministic calculation with banking rounding")
    void calculateBaseCommission_DeterministicRounding() {
        // Given
        BigDecimal guaranteeAmount = new BigDecimal("100000.00");
        BigDecimal commissionRate = new BigDecimal("1.5"); // 1.5%
        BigDecimal minimumCommission = new BigDecimal("50.00");
        
        // When
        BigDecimal result = commissionService.calculateBaseCommission(guaranteeAmount, commissionRate, minimumCommission);
        
        // Then
        assertThat(result).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("T3.2: Respects configured minimum")
    void calculateBaseCommission_AppliesMinimum() {
        // Given
        BigDecimal guaranteeAmount = new BigDecimal("1000.00"); // Small amount
        BigDecimal commissionRate = new BigDecimal("1.5"); // 1.5% = 15.00
        BigDecimal minimumCommission = new BigDecimal("50.00"); // Higher than calculated
        
        // When
        BigDecimal result = commissionService.calculateBaseCommission(guaranteeAmount, commissionRate, minimumCommission);
        
        // Then
        assertThat(result).isEqualTo(new BigDecimal("50.00")); // Should use minimum
    }

    @Test
    @DisplayName("T3.3: Default deferral in N installments according to rule")
    void createInstallmentFees_DefaultInstallments() {
        // Given
        BigDecimal totalCommission = new BigDecimal("1500.00");
        BigDecimal totalCommissionBase = new BigDecimal("1500.00");
        
        // When
        List<FeeItem> installments = commissionService.createInstallmentFees(
                testGuarantee, totalCommission, totalCommissionBase);
        
        // Then
        assertThat(installments).hasSize(4); // Default 4 installments
        
        // Verify installment amounts (should be 375.00 each)
        BigDecimal expectedInstallmentAmount = new BigDecimal("375.00");
        for (int i = 0; i < 3; i++) {
            assertThat(installments.get(i).getAmount()).isEqualTo(expectedInstallmentAmount);
        }
        
        // Last installment might have rounding adjustment
        assertThat(installments.get(3).getAmount()).isEqualTo(expectedInstallmentAmount);
        
        // Verify total equals original amount
        BigDecimal totalInstallmentAmount = installments.stream()
                .map(FeeItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalInstallmentAmount).isEqualTo(totalCommission);
    }

    @Test
    @DisplayName("T3.4: Valid manual installment override with consistent sum")
    void createManualInstallments_ValidSum() {
        // Given
        List<BigDecimal> manualAmounts = List.of(
                new BigDecimal("400.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00")
        );
        List<LocalDate> dueDates = List.of(
                LocalDate.now().plusMonths(3),
                LocalDate.now().plusMonths(6),
                LocalDate.now().plusMonths(9),
                LocalDate.now().plusMonths(12)
        );
        
        // Note: FX service not stubbed because test guarantee is in USD (base currency)
        when(feeItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<FeeItem> result = commissionService.createManualInstallments(testGuarantee, manualAmounts, dueDates);
        
        // Then
        assertThat(result).hasSize(4);
        
        for (int i = 0; i < 4; i++) {
            assertThat(result.get(i).getAmount()).isEqualTo(manualAmounts.get(i));
            assertThat(result.get(i).getDueDate()).isEqualTo(dueDates.get(i));
            assertThat(result.get(i).getIsCalculated()).isFalse(); // Manual entry
        }
        
        verify(feeItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("T3.4: Invalid manual installment - sum mismatch throws exception")
    void createManualInstallments_InvalidSum_ThrowsException() {
        // Given
        List<BigDecimal> invalidAmounts = List.of(
                new BigDecimal("400.00"),
                new BigDecimal("500.00") // Total = 900, but commission should be 1500
        );
        List<LocalDate> dueDates = List.of(
                LocalDate.now().plusMonths(3),
                LocalDate.now().plusMonths(6)
        );
        
        // When & Then
        assertThatThrownBy(() -> commissionService.createManualInstallments(testGuarantee, invalidAmounts, dueDates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manual installment total")
                .hasMessageContaining("must equal calculated commission");
    }

    @Test
    @DisplayName("T3.5: Correct rate selection for Performance guarantee")
    void getCommissionRateForGuarantee_PerformanceType() {
        // Given
        testGuarantee.setGuaranteeType(GuaranteeType.PERFORMANCE);
        
        // When
        BigDecimal rate = commissionService.getCommissionRateForGuarantee(testGuarantee);
        
        // Then
        assertThat(rate).isEqualTo(new BigDecimal("1.5")); // Performance rate
    }

    @Test
    @DisplayName("T3.5: Correct rate selection for Advance Payment guarantee")
    void getCommissionRateForGuarantee_AdvancePaymentType() {
        // Given
        testGuarantee.setGuaranteeType(GuaranteeType.ADVANCE_PAYMENT);
        
        // When
        BigDecimal rate = commissionService.getCommissionRateForGuarantee(testGuarantee);
        
        // Then
        assertThat(rate).isEqualTo(new BigDecimal("2.0")); // Advance payment rate
    }

    @Test
    @DisplayName("T3.6: Calculation uses current day's FX rate in foreign currencies")
    void convertToBaseCurrency_ForeignCurrency() {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");
        String fromCurrency = "EUR";
        BigDecimal expectedExchangeRate = new BigDecimal("1.1000");
        
        when(fxRateService.getExchangeRate("EUR", "USD")).thenReturn(expectedExchangeRate);
        
        // When
        BigDecimal result = commissionService.convertToBaseCurrency(amount, fromCurrency);
        
        // Then
        assertThat(result).isEqualTo(new BigDecimal("1100.00"));
        verify(fxRateService).getExchangeRate("EUR", "USD");
    }

    @Test
    @DisplayName("T3.6: USD currency returns same amount")
    void convertToBaseCurrency_USDCurrency() {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");
        String fromCurrency = "USD";
        
        // When
        BigDecimal result = commissionService.convertToBaseCurrency(amount, fromCurrency);
        
        // Then
        assertThat(result).isEqualTo(amount);
        verify(fxRateService, never()).getExchangeRate(anyString(), anyString());
    }

    @Test
    @DisplayName("T3.6: Controlled error if no rate and manual fallback required")
    void convertToBaseCurrency_NoRateAvailable_ThrowsException() {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");
        String fromCurrency = "EUR";
        
        when(fxRateService.getExchangeRate("EUR", "USD"))
                .thenThrow(new RuntimeException("Exchange rate not available"));
        
        // When & Then
        assertThatThrownBy(() -> commissionService.convertToBaseCurrency(amount, fromCurrency))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get exchange rate for EUR to USD");
    }

    @Test
    @DisplayName("Calculate commission with rounding precision")
    void calculateBaseCommission_PreciseRounding() {
        // Given
        BigDecimal guaranteeAmount = new BigDecimal("333333.33");
        BigDecimal commissionRate = new BigDecimal("1.666666"); // Results in fractional amount
        BigDecimal minimumCommission = new BigDecimal("50.00");
        
        // When
        BigDecimal result = commissionService.calculateBaseCommission(guaranteeAmount, commissionRate, minimumCommission);
        
        // Then - Should be rounded to 2 decimal places with HALF_UP
        // 333333.33 * 1.666666 / 100 = 5555.55499778 -> rounds to 5555.55
        assertThat(result).isEqualTo(new BigDecimal("5555.55"));
        assertThat(result.scale()).isEqualTo(2);
    }
}
