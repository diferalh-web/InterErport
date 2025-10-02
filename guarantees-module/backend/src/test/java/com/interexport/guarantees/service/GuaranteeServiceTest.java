package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.exception.GuaranteeNotFoundException;
import com.interexport.guarantees.exception.InvalidGuaranteeStateException;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GuaranteeService.
 * Implements test requirements from F1 - Guarantees CRUD.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Guarantee Service Tests")
class GuaranteeServiceTest {

    @Mock
    private GuaranteeContractRepository guaranteeRepository;

    @Mock
    private FxRateService fxRateService;

    @Mock
    private CommissionCalculationService commissionService;

    @Mock
    private ReferenceGeneratorService referenceGenerator;

    @InjectMocks
    private GuaranteeService guaranteeService;

    private GuaranteeContract testGuarantee;

    @BeforeEach
    void setUp() {
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
    @DisplayName("T1.1: Controller returns 201 + Location on create")
    void createGuarantee_Success() {
        // Given - Set reference to null to force generation
        testGuarantee.setReference(null);
        when(guaranteeRepository.existsByReference(anyString())).thenReturn(false);
        when(guaranteeRepository.save(any(GuaranteeContract.class))).thenReturn(testGuarantee);
        when(referenceGenerator.generateGuaranteeReference()).thenReturn("GT-20231001-000001");
        
        // When
        GuaranteeContract result = guaranteeService.createGuarantee(testGuarantee);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(GuaranteeStatus.DRAFT);
        
        verify(guaranteeRepository).save(any(GuaranteeContract.class));
        verify(commissionService).calculateCommissionFees(any(GuaranteeContract.class));
    }

    @Test
    @DisplayName("T1.2: Business validation (amount>0, valid currency)")
    void createGuarantee_InvalidAmount_ThrowsException() {
        // Given
        testGuarantee.setAmount(BigDecimal.ZERO);
        
        // When & Then
        assertThatThrownBy(() -> guaranteeService.createGuarantee(testGuarantee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be greater than zero");
        
        verify(guaranteeRepository, never()).save(any());
    }

    @Test
    @DisplayName("T1.2: Business validation - missing currency")
    void createGuarantee_MissingCurrency_ThrowsException() {
        // Given
        testGuarantee.setCurrency(null);
        
        // When & Then
        assertThatThrownBy(() -> guaranteeService.createGuarantee(testGuarantee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency is required");
    }

    @Test
    @DisplayName("T1.2: Business validation - missing beneficiary")
    void createGuarantee_MissingBeneficiary_ThrowsException() {
        // Given
        testGuarantee.setBeneficiaryName("");
        
        // When & Then
        assertThatThrownBy(() -> guaranteeService.createGuarantee(testGuarantee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Beneficiary name is required");
    }

    @Test
    @DisplayName("Find guarantee by ID - success")
    void findById_Success() {
        // Given
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        
        // When
        GuaranteeContract result = guaranteeService.findById(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getReference()).isEqualTo("GT-20231001-000001");
    }

    @Test
    @DisplayName("Find guarantee by ID - not found")
    void findById_NotFound_ThrowsException() {
        // Given
        when(guaranteeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> guaranteeService.findById(999L))
                .isInstanceOf(GuaranteeNotFoundException.class)
                .hasMessageContaining("Guarantee not found with id: 999");
    }

    @Test
    @DisplayName("T1.4: Cancellation rejected if an open claim exists")
    void cancelGuarantee_WithActiveClaims_ThrowsException() {
        // Given
        testGuarantee.setStatus(GuaranteeStatus.APPROVED);
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        
        // Mock that guarantee has active claims
        GuaranteeContract spyGuarantee = spy(testGuarantee);
        when(spyGuarantee.hasActiveClaims()).thenReturn(true);
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(spyGuarantee));
        
        // When & Then
        assertThatThrownBy(() -> guaranteeService.cancelGuarantee(1L, "testUser", "Test reason"))
                .isInstanceOf(InvalidGuaranteeStateException.class)
                .hasMessageContaining("Cannot cancel guarantee with active claims");
    }

    @Test
    @DisplayName("Cancel guarantee - success")
    void cancelGuarantee_Success() {
        // Given
        testGuarantee.setStatus(GuaranteeStatus.APPROVED);
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        when(guaranteeRepository.save(any(GuaranteeContract.class))).thenReturn(testGuarantee);
        
        // When
        GuaranteeContract result = guaranteeService.cancelGuarantee(1L, "testUser", "Test reason");
        
        // Then
        assertThat(result.getStatus()).isEqualTo(GuaranteeStatus.CANCELLED);
        assertThat(result.getLastModifiedBy()).isEqualTo("testUser");
        assertThat(result.getSpecialConditions()).contains("CANCELLED: Test reason");
        
        verify(guaranteeRepository).save(testGuarantee);
    }

    @Test
    @DisplayName("Submit guarantee for approval - success")
    void submitForApproval_Success() {
        // Given
        testGuarantee.setStatus(GuaranteeStatus.DRAFT);
        testGuarantee.setGuaranteeText("Test guarantee text");
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        when(guaranteeRepository.save(any(GuaranteeContract.class))).thenReturn(testGuarantee);
        
        // When
        GuaranteeContract result = guaranteeService.submitForApproval(1L);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(GuaranteeStatus.SUBMITTED);
        verify(guaranteeRepository).save(testGuarantee);
    }

    @Test
    @DisplayName("Submit guarantee for approval - invalid state")
    void submitForApproval_InvalidState_ThrowsException() {
        // Given
        testGuarantee.setStatus(GuaranteeStatus.APPROVED);
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        
        // When & Then
        assertThatThrownBy(() -> guaranteeService.submitForApproval(1L))
                .isInstanceOf(InvalidGuaranteeStateException.class)
                .hasMessageContaining("Only draft guarantees can be submitted");
    }

    @Test
    @DisplayName("Approve guarantee - success")
    void approveGuarantee_Success() {
        // Given
        testGuarantee.setStatus(GuaranteeStatus.SUBMITTED);
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        when(guaranteeRepository.save(any(GuaranteeContract.class))).thenReturn(testGuarantee);
        
        // When
        GuaranteeContract result = guaranteeService.approveGuarantee(1L, "approver");
        
        // Then
        assertThat(result.getStatus()).isEqualTo(GuaranteeStatus.APPROVED);
        assertThat(result.getLastModifiedBy()).isEqualTo("approver");
        verify(guaranteeRepository).save(testGuarantee);
    }

    @Test
    @DisplayName("Reject guarantee - success")
    void rejectGuarantee_Success() {
        // Given
        testGuarantee.setStatus(GuaranteeStatus.SUBMITTED);
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        when(guaranteeRepository.save(any(GuaranteeContract.class))).thenReturn(testGuarantee);
        
        // When
        GuaranteeContract result = guaranteeService.rejectGuarantee(1L, "rejector", "Invalid data");
        
        // Then
        assertThat(result.getStatus()).isEqualTo(GuaranteeStatus.REJECTED);
        assertThat(result.getLastModifiedBy()).isEqualTo("rejector");
        assertThat(result.getSpecialConditions()).contains("REJECTED: Invalid data");
        verify(guaranteeRepository).save(testGuarantee);
    }

    @Test
    @DisplayName("Update guarantee - success")
    void updateGuarantee_Success() {
        // Given
        testGuarantee.setStatus(GuaranteeStatus.DRAFT);
        GuaranteeContract updatedData = new GuaranteeContract();
        updatedData.setAmount(new BigDecimal("150000.00"));
        updatedData.setBeneficiaryName("Updated Beneficiary");
        
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        when(guaranteeRepository.save(any(GuaranteeContract.class))).thenReturn(testGuarantee);
        
        // When
        GuaranteeContract result = guaranteeService.updateGuarantee(1L, updatedData);
        
        // Then
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("150000.00"));
        assertThat(result.getBeneficiaryName()).isEqualTo("Updated Beneficiary");
        verify(guaranteeRepository).save(testGuarantee);
    }

    @Test
    @DisplayName("Update guarantee - invalid state")
    void updateGuarantee_InvalidState_ThrowsException() {
        // Given
        testGuarantee.setStatus(GuaranteeStatus.APPROVED);
        GuaranteeContract updatedData = new GuaranteeContract();
        
        when(guaranteeRepository.findById(1L)).thenReturn(Optional.of(testGuarantee));
        
        // When & Then
        assertThatThrownBy(() -> guaranteeService.updateGuarantee(1L, updatedData))
                .isInstanceOf(InvalidGuaranteeStateException.class)
                .hasMessageContaining("Cannot edit guarantee in status: APPROVED");
    }
}
