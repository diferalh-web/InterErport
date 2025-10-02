package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.exception.GuaranteeNotFoundException;
import com.interexport.guarantees.exception.InvalidGuaranteeStateException;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service class for Guarantee Contract operations.
 * Implements business logic for F1 - Guarantees CRUD and related functionality.
 */
@Service
@Transactional
public class GuaranteeService {

    private final GuaranteeContractRepository guaranteeRepository;
    private final FxRateService fxRateService;
    private final CommissionCalculationService commissionService;
    private final ReferenceGeneratorService referenceGenerator;

    @Autowired
    public GuaranteeService(GuaranteeContractRepository guaranteeRepository,
                          FxRateService fxRateService,
                          CommissionCalculationService commissionService,
                          ReferenceGeneratorService referenceGenerator) {
        this.guaranteeRepository = guaranteeRepository;
        this.fxRateService = fxRateService;
        this.commissionService = commissionService;
        this.referenceGenerator = referenceGenerator;
    }

    /**
     * Create a new guarantee contract
     * UC1.1: Create international guarantee with 22A/22D/40D and participants
     */
    public GuaranteeContract createGuarantee(GuaranteeContract guarantee) {
        // Validate business rules
        validateGuaranteeCreation(guarantee);
        
        // Generate reference if not provided
        if (guarantee.getReference() == null || guarantee.getReference().trim().isEmpty()) {
            guarantee.setReference(referenceGenerator.generateGuaranteeReference());
        }
        
        // Ensure unique reference
        if (guaranteeRepository.existsByReference(guarantee.getReference())) {
            throw new IllegalArgumentException("Guarantee reference already exists: " + guarantee.getReference());
        }
        
        // Calculate base currency equivalent if needed
        calculateBaseCurrencyAmount(guarantee);
        
        // Set initial status if not set
        if (guarantee.getStatus() == null) {
            guarantee.setStatus(GuaranteeStatus.DRAFT);
        }
        
        GuaranteeContract savedGuarantee = guaranteeRepository.save(guarantee);
        
        // Calculate and create commission fees
        commissionService.calculateCommissionFees(savedGuarantee);
        
        return savedGuarantee;
    }

    /**
     * Update an existing guarantee
     * UC1.3: Update allowed fields prior to approval with diff logging
     */
    public GuaranteeContract updateGuarantee(Long id, GuaranteeContract updatedGuarantee) {
        GuaranteeContract existingGuarantee = findById(id);
        
        // Check if guarantee can be edited
        if (!existingGuarantee.getStatus().isEditable()) {
            throw new InvalidGuaranteeStateException(
                "Cannot edit guarantee in status: " + existingGuarantee.getStatus());
        }
        
        // Validate business rules
        validateGuaranteeUpdate(existingGuarantee, updatedGuarantee);
        
        // Update allowed fields
        updateAllowedFields(existingGuarantee, updatedGuarantee);
        
        // Recalculate base currency amount if currency or amount changed
        calculateBaseCurrencyAmount(existingGuarantee);
        
        return guaranteeRepository.save(existingGuarantee);
    }

    /**
     * Find guarantee by ID
     */
    @Transactional(readOnly = true)
    public GuaranteeContract findById(Long id) {
        return guaranteeRepository.findById(id)
                .orElseThrow(() -> new GuaranteeNotFoundException("Guarantee not found with id: " + id));
    }

    /**
     * Find guarantee by reference
     */
    @Transactional(readOnly = true)
    public Optional<GuaranteeContract> findByReference(String reference) {
        return guaranteeRepository.findByReference(reference);
    }

    /**
     * Search guarantees with pagination
     * UC1.5: Query by reference/date/status with pagination
     */
    @Transactional(readOnly = true)
    public Page<GuaranteeContract> searchGuarantees(String reference, GuaranteeStatus status,
                                                   GuaranteeType guaranteeType, Long applicantId,
                                                   String currency, LocalDate fromDate, LocalDate toDate,
                                                   Pageable pageable) {
        return guaranteeRepository.findByCriteria(reference, status, guaranteeType, 
                                                 applicantId, currency, fromDate, toDate, pageable);
    }

    /**
     * Get all guarantees with pagination
     */
    @Transactional(readOnly = true)
    public Page<GuaranteeContract> findAll(Pageable pageable) {
        return guaranteeRepository.findAll(pageable);
    }

    /**
     * Submit guarantee for approval
     */
    public GuaranteeContract submitForApproval(Long id) {
        GuaranteeContract guarantee = findById(id);
        
        if (guarantee.getStatus() != GuaranteeStatus.DRAFT) {
            throw new InvalidGuaranteeStateException(String.format(
                "Cannot submit guarantee %s for approval - only DRAFT guarantees can be submitted. " +
                "This guarantee currently has status '%s'. " +
                "Draft guarantees can be submitted, submitted guarantees can be approved or rejected.",
                guarantee.getReference(), guarantee.getStatus()));
        }
        
        // Validate completeness
        validateGuaranteeCompleteness(guarantee);
        
        guarantee.setStatus(GuaranteeStatus.SUBMITTED);
        return guaranteeRepository.save(guarantee);
    }

    /**
     * Approve guarantee
     */
    public GuaranteeContract approveGuarantee(Long id, String approvedBy) {
        GuaranteeContract guarantee = findById(id);
        
        if (guarantee.getStatus() != GuaranteeStatus.SUBMITTED) {
            throw new InvalidGuaranteeStateException(
                "Only submitted guarantees can be approved");
        }
        
        guarantee.setStatus(GuaranteeStatus.APPROVED);
        guarantee.setLastModifiedBy(approvedBy);
        
        return guaranteeRepository.save(guarantee);
    }

    /**
     * Reject guarantee
     */
    public GuaranteeContract rejectGuarantee(Long id, String rejectedBy, String reason) {
        GuaranteeContract guarantee = findById(id);
        
        if (guarantee.getStatus() != GuaranteeStatus.SUBMITTED) {
            throw new InvalidGuaranteeStateException(
                "Only submitted guarantees can be rejected");
        }
        
        guarantee.setStatus(GuaranteeStatus.REJECTED);
        guarantee.setLastModifiedBy(rejectedBy);
        // Store rejection reason in special conditions or create audit log
        guarantee.setSpecialConditions(
            (guarantee.getSpecialConditions() != null ? guarantee.getSpecialConditions() + "\n" : "")
            + "REJECTED: " + reason);
        
        return guaranteeRepository.save(guarantee);
    }

    /**
     * Cancel guarantee
     * UC1.4: Cancel guarantee with validation of open claims
     */
    public GuaranteeContract cancelGuarantee(Long id, String cancelledBy, String reason) {
        GuaranteeContract guarantee = findById(id);
        
        if (!guarantee.getStatus().isCancellable()) {
            throw new InvalidGuaranteeStateException(
                "Cannot cancel guarantee in status: " + guarantee.getStatus());
        }
        
        // Check for active claims
        if (guarantee.hasActiveClaims()) {
            throw new InvalidGuaranteeStateException(
                "Cannot cancel guarantee with active claims");
        }
        
        guarantee.setStatus(GuaranteeStatus.CANCELLED);
        guarantee.setLastModifiedBy(cancelledBy);
        guarantee.setSpecialConditions(
            (guarantee.getSpecialConditions() != null ? guarantee.getSpecialConditions() + "\n" : "")
            + "CANCELLED: " + reason);
        
        return guaranteeRepository.save(guarantee);
    }

    /**
     * Get guarantees expiring soon (for alerts)
     */
    @Transactional(readOnly = true)
    public List<GuaranteeContract> findExpiringSoon(int daysAhead) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(daysAhead);
        return guaranteeRepository.findExpiringSoon(startDate, endDate);
    }

    /**
     * Get guarantee with full details (including amendments, claims, fees)
     */
    @Transactional(readOnly = true)
    public Optional<GuaranteeContract> findByIdWithDetails(Long id) {
        return guaranteeRepository.findByIdWithDetails(id);
    }

    /**
     * Get total outstanding amount by currency
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTotalOutstandingAmountByCurrency() {
        return guaranteeRepository.getTotalOutstandingAmountByCurrency();
    }

    /**
     * Get total outstanding amount in base currency
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingAmountInBaseCurrency() {
        BigDecimal total = guaranteeRepository.getTotalOutstandingAmountInBaseCurrency();
        return total != null ? total : BigDecimal.ZERO;
    }

    // Private helper methods

    private void validateGuaranteeCreation(GuaranteeContract guarantee) {
        if (guarantee.getAmount() == null || guarantee.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Guarantee amount must be greater than zero");
        }
        
        if (guarantee.getCurrency() == null || guarantee.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }
        
        if (guarantee.getExpiryDate() == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        
        if (guarantee.getIssueDate() == null) {
            guarantee.setIssueDate(LocalDate.now());
        }
        
        if (guarantee.getExpiryDate().isBefore(guarantee.getIssueDate())) {
            throw new IllegalArgumentException("Expiry date cannot be before issue date");
        }
        
        if (guarantee.getApplicantId() == null) {
            throw new IllegalArgumentException("Applicant is required");
        }
        
        if (guarantee.getBeneficiaryName() == null || guarantee.getBeneficiaryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Beneficiary name is required");
        }
    }

    private void validateGuaranteeUpdate(GuaranteeContract existing, GuaranteeContract updated) {
        // Business validation for updates
        if (updated.getAmount() != null && updated.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Guarantee amount must be greater than zero");
        }
        
        if (updated.getExpiryDate() != null && updated.getExpiryDate().isBefore(existing.getIssueDate())) {
            throw new IllegalArgumentException("Expiry date cannot be before issue date");
        }
    }

    private void updateAllowedFields(GuaranteeContract existing, GuaranteeContract updated) {
        // Only update fields that are allowed to be modified in current status
        if (updated.getAmount() != null) existing.setAmount(updated.getAmount());
        if (updated.getCurrency() != null) existing.setCurrency(updated.getCurrency());
        if (updated.getExpiryDate() != null) existing.setExpiryDate(updated.getExpiryDate());
        if (updated.getBeneficiaryName() != null) existing.setBeneficiaryName(updated.getBeneficiaryName());
        if (updated.getBeneficiaryAddress() != null) existing.setBeneficiaryAddress(updated.getBeneficiaryAddress());
        if (updated.getGuaranteeText() != null) existing.setGuaranteeText(updated.getGuaranteeText());
        if (updated.getSpecialConditions() != null) existing.setSpecialConditions(updated.getSpecialConditions());
        if (updated.getUnderlyingContractRef() != null) existing.setUnderlyingContractRef(updated.getUnderlyingContractRef());
        if (updated.getAdvisingBankBic() != null) existing.setAdvisingBankBic(updated.getAdvisingBankBic());
        if (updated.getIsDomestic() != null) existing.setIsDomestic(updated.getIsDomestic());
    }

    private void calculateBaseCurrencyAmount(GuaranteeContract guarantee) {
        if (!"USD".equals(guarantee.getCurrency())) {
            try {
                BigDecimal exchangeRate = fxRateService.getExchangeRate(guarantee.getCurrency(), "USD");
                guarantee.setExchangeRate(exchangeRate);
                guarantee.setBaseAmount(guarantee.getAmount().multiply(exchangeRate)
                    .setScale(2, java.math.RoundingMode.HALF_UP));
            } catch (Exception e) {
                // Log warning but don't fail the operation
                guarantee.setBaseAmount(null);
                guarantee.setExchangeRate(null);
            }
        } else {
            guarantee.setBaseAmount(guarantee.getAmount());
            guarantee.setExchangeRate(BigDecimal.ONE);
        }
    }

    private void validateGuaranteeCompleteness(GuaranteeContract guarantee) {
        if (guarantee.getGuaranteeText() == null || guarantee.getGuaranteeText().trim().isEmpty()) {
            throw new IllegalStateException(String.format(
                "Cannot submit guarantee %s - guarantee text is required. " +
                "Please edit the guarantee and add the guarantee text before submitting for approval. " +
                "The guarantee text should contain the complete guarantee wording that will be issued to the beneficiary.",
                guarantee.getReference()));
        }
    }
}
