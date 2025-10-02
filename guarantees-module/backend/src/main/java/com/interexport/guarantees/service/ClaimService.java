package com.interexport.guarantees.service;

import com.interexport.guarantees.dto.ClaimCreateRequest;
import com.interexport.guarantees.entity.Claim;
import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.ClaimStatus;
import com.interexport.guarantees.exception.ClaimNotFoundException;
import com.interexport.guarantees.exception.GuaranteeNotFoundException;
import com.interexport.guarantees.repository.ClaimRepository;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Guarantee Claims (F6)
 * Handles claim processing including validation, approval, rejection, and settlement
 * 
 * Business Rules:
 * - Claim amount cannot exceed remaining guarantee amount
 * - Claims require supporting documentation
 * - Approved claims can be settled with payment
 * - Settlement can be in different currency with FX conversion
 */
@Service
@Transactional
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final GuaranteeContractRepository guaranteeRepository;
    private final ReferenceGeneratorService referenceGenerator;
    private final FxRateService fxRateService;

    @Autowired
    public ClaimService(
            ClaimRepository claimRepository,
            GuaranteeContractRepository guaranteeRepository,
            ReferenceGeneratorService referenceGenerator,
            FxRateService fxRateService) {
        this.claimRepository = claimRepository;
        this.guaranteeRepository = guaranteeRepository;
        this.referenceGenerator = referenceGenerator;
        this.fxRateService = fxRateService;
    }

    @Transactional(readOnly = true)
    public Page<Claim> findByGuaranteeId(Long guaranteeId, Pageable pageable) {
        return claimRepository.findByGuaranteeIdOrderByClaimDateDesc(guaranteeId, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Claim> findById(Long id) {
        return claimRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Claim> findByGuaranteeIdAndStatus(Long guaranteeId, ClaimStatus status) {
        return claimRepository.findByGuaranteeIdAndStatus(guaranteeId, status);
    }

    @Transactional(readOnly = true)
    public List<Claim> findPendingClaimsByGuaranteeId(Long guaranteeId) {
        return claimRepository.findByGuaranteeIdAndStatusIn(guaranteeId, 
                Arrays.asList(ClaimStatus.REQUESTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.PENDING_DOCUMENTS));
    }

    @Transactional(readOnly = true)
    public List<Claim> findByGuaranteeIdAndAmountRange(Long guaranteeId, BigDecimal minAmount, BigDecimal maxAmount) {
        return claimRepository.findByGuaranteeIdAndAmountBetween(guaranteeId, minAmount, maxAmount);
    }

    @Transactional
    public Claim createClaimFromRequest(Long guaranteeId, ClaimCreateRequest request) {
        // Create claim entity from request
        Claim claim = new Claim();
        claim.setClaimReference(request.getClaimReference());
        claim.setAmount(request.getAmount());
        claim.setCurrency(request.getCurrency());
        claim.setClaimDate(request.getClaimDate());
        claim.setClaimReason(request.getClaimReason());
        claim.setBeneficiaryContact(request.getBeneficiaryContact());
        claim.setProcessingDeadline(request.getProcessingDeadline());
        claim.setProcessingNotes(request.getProcessingNotes());
        claim.setRequiresSpecialApproval(Boolean.TRUE.equals(request.getRequiresSpecialApproval()));
        claim.setDocumentsSubmitted(Boolean.TRUE.equals(request.getDocumentsSubmitted()) ? "Yes" : "No");

        return createClaim(guaranteeId, claim);
    }

    @Transactional
    public Claim createClaim(Long guaranteeId, Claim claim) {
        // Verify guarantee exists and is valid for claims
        GuaranteeContract guarantee = guaranteeRepository.findById(guaranteeId)
                .orElseThrow(() -> new GuaranteeNotFoundException("Guarantee not found with id: " + guaranteeId));

        validateGuaranteeForClaim(guarantee);

        // Set guarantee relationship
        claim.setGuarantee(guarantee);

        // Generate claim reference if not provided
        if (claim.getClaimReference() == null || claim.getClaimReference().isEmpty()) {
            claim.setClaimReference(referenceGenerator.generateClaimReference(guarantee.getReference()));
        }

        // Validate claim amount against available guarantee amount
        validateClaimAmount(claim, guarantee);

        // Set default values
        if (claim.getClaimDate() == null) {
            claim.setClaimDate(LocalDate.now());
        }
        if (claim.getCurrency() == null || claim.getCurrency().isEmpty()) {
            claim.setCurrency(guarantee.getCurrency());
        }

        // Set initial status
        claim.setStatus(ClaimStatus.REQUESTED);

        // Set processing deadline (default 30 days from claim date)
        if (claim.getProcessingDeadline() == null) {
            claim.setProcessingDeadline(LocalDate.now().plusDays(30));
        }

        return claimRepository.save(claim);
    }

    @Transactional
    public Claim updateClaim(Long claimId, Claim updatedClaim) {
        Claim existingClaim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found with id: " + claimId));

        // Only allow updates if claim is still editable
        if (!isClaimEditable(existingClaim.getStatus())) {
            throw new IllegalArgumentException("Cannot update claim in status: " + existingClaim.getStatus());
        }

        // Update modifiable fields
        existingClaim.setAmount(updatedClaim.getAmount());
        existingClaim.setClaimReason(updatedClaim.getClaimReason());
        existingClaim.setDocumentsSubmitted(updatedClaim.getDocumentsSubmitted());
        existingClaim.setBeneficiaryContact(updatedClaim.getBeneficiaryContact());
        existingClaim.setProcessingNotes(updatedClaim.getProcessingNotes());

        // Re-validate claim amount
        validateClaimAmount(existingClaim, existingClaim.getGuarantee());

        return claimRepository.save(existingClaim);
    }

    @Transactional
    public Claim approveClaim(Long claimId, String comments) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found with id: " + claimId));

        // Validate claim can be approved
        if (claim.getStatus() != ClaimStatus.REQUESTED && claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new IllegalArgumentException("Cannot approve claim in status: " + claim.getStatus());
        }

        // Check if all required documents are submitted
        if (claim.getDocumentsSubmitted() == null || claim.getDocumentsSubmitted().toLowerCase().contains("pending")) {
            claim.setStatus(ClaimStatus.PENDING_DOCUMENTS);
            claim.setMissingDocuments("Required documentation not submitted");
        } else {
            claim.setStatus(ClaimStatus.APPROVED);
            claim.setApprovedDate(LocalDateTime.now());
            claim.setApprovedBy(getCurrentUsername()); // Get from security context
        }

        claim.setProcessingNotes(comments);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim rejectClaim(Long claimId, String reason) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found with id: " + claimId));

        claim.setStatus(ClaimStatus.REJECTED);
        claim.setRejectedDate(LocalDateTime.now());
        claim.setRejectedBy(getCurrentUsername()); // TODO: Get from security context
        claim.setRejectionReason(reason);

        return claimRepository.save(claim);
    }

    @Transactional
    public Claim settleClaim(Long claimId, BigDecimal paymentAmount, String paymentReference, String paymentCurrency) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found with id: " + claimId));

        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot settle claim that is not approved");
        }

        claim.setStatus(ClaimStatus.SETTLED);
        claim.setPaymentDate(LocalDateTime.now());
        claim.setPaymentReference(paymentReference);

        // Handle currency conversion if needed
        if (paymentCurrency != null && !paymentCurrency.equals(claim.getCurrency())) {
            // Convert payment amount to claim currency for recording
            FxRateService.ConversionResult conversion = fxRateService.convertAmountWithDetails(
                    paymentAmount, paymentCurrency, claim.getCurrency());
            // Store both original payment amount and converted amount in notes
            claim.setProcessingNotes(String.format("Payment: %s %s (converted from %s %s at rate %s)", 
                    conversion.getConvertedAmount(), claim.getCurrency(),
                    paymentAmount, paymentCurrency, conversion.getExchangeRate()));
        }

        return claimRepository.save(claim);
    }

    @Transactional
    public Claim requestAdditionalDocuments(Long claimId, String missingDocuments, LocalDate deadline) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found with id: " + claimId));

        claim.setStatus(ClaimStatus.PENDING_DOCUMENTS);
        claim.setMissingDocuments(missingDocuments);
        claim.setProcessingDeadline(deadline);

        return claimRepository.save(claim);
    }

    @Transactional
    public void cancelClaim(Long claimId, String reason) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found with id: " + claimId));

        if (claim.getStatus() == ClaimStatus.SETTLED) {
            throw new IllegalArgumentException("Cannot cancel settled claim");
        }

        claim.setStatus(ClaimStatus.REJECTED);
        claim.setRejectedDate(LocalDateTime.now());
        claim.setRejectedBy(getCurrentUsername()); // TODO: Get from security context
        claim.setRejectionReason("Cancelled: " + reason);

        claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public ClaimSummary getClaimsSummary(Long guaranteeId) {
        GuaranteeContract guarantee = guaranteeRepository.findById(guaranteeId)
                .orElseThrow(() -> new GuaranteeNotFoundException("Guarantee not found with id: " + guaranteeId));

        List<Claim> allClaims = claimRepository.findByGuaranteeId(guaranteeId);
        
        BigDecimal totalClaimed = allClaims.stream()
                .filter(c -> c.getStatus() != ClaimStatus.REJECTED)
                .map(Claim::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSettled = allClaims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.SETTLED)
                .map(Claim::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = allClaims.stream()
                .filter(c -> Arrays.asList(ClaimStatus.REQUESTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.PENDING_DOCUMENTS)
                        .contains(c.getStatus()))
                .count();

        BigDecimal remainingAmount = guarantee.getAmount().subtract(totalClaimed);

        return new ClaimSummary(
                allClaims.size(),
                totalClaimed,
                totalSettled,
                remainingAmount,
                (int) pendingCount,
                guarantee.getCurrency()
        );
    }

    private void validateGuaranteeForClaim(GuaranteeContract guarantee) {
        // Check guarantee status allows claims
        if (guarantee.getStatus() != com.interexport.guarantees.entity.enums.GuaranteeStatus.APPROVED) {
            throw new IllegalArgumentException(String.format(
                "Claims can only be made against APPROVED guarantees. " +
                "This guarantee (%s) has status '%s'. " +
                "Please ensure the guarantee is approved before creating claims.",
                guarantee.getReference(), guarantee.getStatus()));
        }

        // Check guarantee is not expired
        if (guarantee.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(String.format(
                "Cannot create claims against expired guarantees. " +
                "Guarantee %s expired on %s. " +
                "Contact your administrator if you need to process claims on expired guarantees.",
                guarantee.getReference(), guarantee.getExpiryDate()));
        }
    }

    private void validateClaimAmount(Claim claim, GuaranteeContract guarantee) {
        // Calculate total existing claims (excluding rejected)
        BigDecimal existingClaims = claimRepository.findByGuaranteeId(guarantee.getId()).stream()
                .filter(c -> c.getStatus() != ClaimStatus.REJECTED && !c.getId().equals(claim.getId()))
                .map(Claim::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithNewClaim = existingClaims.add(claim.getAmount());

        if (totalWithNewClaim.compareTo(guarantee.getAmount()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Claim amount exceeds available guarantee funds. " +
                    "Total claim amount would be %s %s but guarantee %s only has %s %s available " +
                    "(Original amount: %s %s, Existing claims: %s %s). " +
                    "Please reduce the claim amount to %s %s or less.",
                    totalWithNewClaim, guarantee.getCurrency(),
                    guarantee.getReference(),
                    guarantee.getAmount().subtract(existingClaims), guarantee.getCurrency(),
                    guarantee.getAmount(), guarantee.getCurrency(),
                    existingClaims, guarantee.getCurrency(),
                    guarantee.getAmount().subtract(existingClaims), guarantee.getCurrency()));
        }
    }

    private boolean isClaimEditable(ClaimStatus status) {
        return status == ClaimStatus.REQUESTED || status == ClaimStatus.PENDING_DOCUMENTS;
    }

    /**
     * Get current authenticated username from security context
     */
    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM"; // Fallback for non-authenticated contexts
        }
    }

    /**
     * Summary of claims for a guarantee
     */
    public static class ClaimSummary {
        private final int totalClaims;
        private final BigDecimal totalClaimedAmount;
        private final BigDecimal totalSettledAmount;
        private final BigDecimal remainingGuaranteeAmount;
        private final int pendingClaims;
        private final String currency;

        public ClaimSummary(int totalClaims, BigDecimal totalClaimedAmount, 
                          BigDecimal totalSettledAmount, BigDecimal remainingGuaranteeAmount,
                          int pendingClaims, String currency) {
            this.totalClaims = totalClaims;
            this.totalClaimedAmount = totalClaimedAmount;
            this.totalSettledAmount = totalSettledAmount;
            this.remainingGuaranteeAmount = remainingGuaranteeAmount;
            this.pendingClaims = pendingClaims;
            this.currency = currency;
        }

        // Getters
        public int getTotalClaims() { return totalClaims; }
        public BigDecimal getTotalClaimedAmount() { return totalClaimedAmount; }
        public BigDecimal getTotalSettledAmount() { return totalSettledAmount; }
        public BigDecimal getRemainingGuaranteeAmount() { return remainingGuaranteeAmount; }
        public int getPendingClaims() { return pendingClaims; }
        public String getCurrency() { return currency; }
    }
}
