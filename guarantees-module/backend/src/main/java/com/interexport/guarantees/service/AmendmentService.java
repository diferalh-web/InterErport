package com.interexport.guarantees.service;

import com.interexport.guarantees.dto.AmendmentCreateRequest;
import com.interexport.guarantees.entity.Amendment;
import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.AmendmentType;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.exception.AmendmentNotFoundException;
import com.interexport.guarantees.exception.GuaranteeNotFoundException;
import com.interexport.guarantees.repository.AmendmentRepository;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Guarantee Amendments (F5)
 * Handles amendments with consent (GITRAM) and without consent (GITAME)
 * 
 * Business Rules:
 * - Amount increases require consent
 * - Expiry extensions require consent  
 * - Text/condition changes may not require consent
 * - Auto-approval for amendments under configured threshold
 */
@Service
@Transactional
public class AmendmentService {

    private static final Logger logger = LoggerFactory.getLogger(AmendmentService.class);
    
    private final AmendmentRepository amendmentRepository;
    private final GuaranteeContractRepository guaranteeRepository;
    private final ReferenceGeneratorService referenceGenerator;

    // Business rule thresholds
    private static final double AUTO_APPROVE_THRESHOLD = 10000.0; // Auto-approve amendments under this amount

    @Autowired
    public AmendmentService(
            AmendmentRepository amendmentRepository,
            GuaranteeContractRepository guaranteeRepository,
            ReferenceGeneratorService referenceGenerator) {
        this.amendmentRepository = amendmentRepository;
        this.guaranteeRepository = guaranteeRepository;
        this.referenceGenerator = referenceGenerator;
    }

    @Transactional(readOnly = true)
    public Page<Amendment> findByGuaranteeId(Long guaranteeId, Pageable pageable) {
        return amendmentRepository.findByGuaranteeIdOrderByCreatedDateDesc(guaranteeId, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Amendment> findById(Long id) {
        return amendmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Amendment> findByGuaranteeIdAndStatus(Long guaranteeId, GuaranteeStatus status) {
        return amendmentRepository.findByGuaranteeIdAndStatus(guaranteeId, status);
    }

    @Transactional(readOnly = true)
    public List<Amendment> findByGuaranteeIdAndType(Long guaranteeId, AmendmentType type) {
        return amendmentRepository.findByGuaranteeIdAndAmendmentType(guaranteeId, type);
    }

    /**
     * Create a new amendment for a guarantee using DTO
     */
    @Transactional
    public Amendment createAmendment(Long guaranteeId, AmendmentCreateRequest request) {
        // Find the guarantee
        GuaranteeContract guarantee = guaranteeRepository.findById(guaranteeId)
                .orElseThrow(() -> new GuaranteeNotFoundException("Guarantee not found with id: " + guaranteeId));
        
        // Create amendment from DTO
        Amendment amendment = new Amendment();
        amendment.setGuarantee(guarantee);
        amendment.setAmendmentType(request.getAmendmentType());
        amendment.setDescription(request.getDescription());
        amendment.setReason(request.getReason());
        amendment.setChangesJson(request.getChangesJson());
        
        // Set consent requirement from request or let entity determine automatically
        if (request.getRequiresConsent() != null) {
            amendment.setRequiresConsent(request.getRequiresConsent());
        }
        // Note: The Amendment entity will automatically determine consent requirement in setAmendmentType
        
        return createAmendment(guaranteeId, amendment);
    }

    @Transactional
    public Amendment createAmendment(Long guaranteeId, Amendment amendment) {
        // Verify guarantee exists
        GuaranteeContract guarantee = guaranteeRepository.findById(guaranteeId)
                .orElseThrow(() -> new GuaranteeNotFoundException("Guarantee not found with id: " + guaranteeId));

        // Validate guarantee can be amended
        if (guarantee.getStatus() != GuaranteeStatus.APPROVED) {
            throw new IllegalArgumentException(String.format(
                "Cannot create amendment for guarantee %s - only APPROVED guarantees can be amended. " +
                "This guarantee currently has status '%s'. " +
                "Please ensure the guarantee is approved before creating amendments.",
                guarantee.getReference(), guarantee.getStatus()));
        }

        // Set guarantee relationship
        amendment.setGuarantee(guarantee);

        // Generate amendment reference
        if (amendment.getAmendmentReference() == null || amendment.getAmendmentReference().isEmpty()) {
            amendment.setAmendmentReference(referenceGenerator.generateAmendmentReference(guarantee.getReference()));
        }

        // Determine if consent is required based on business rules
        if (amendment.getRequiresConsent() == null) {
            amendment.setRequiresConsent(determineConsentRequired(amendment));
        }

        // Set default amendment type if not specified
        if (amendment.getAmendmentType() == null) {
            amendment.setAmendmentType(AmendmentType.OTHER);
        }

        // Set initial status
        amendment.setStatus(GuaranteeStatus.DRAFT);
        amendment.setSubmittedDate(LocalDateTime.now());

        // Auto-approve if under threshold and no consent required
        if (!amendment.getRequiresConsent() && isUnderAutoApprovalThreshold(amendment)) {
            amendment.setStatus(GuaranteeStatus.APPROVED);
            amendment.setProcessedDate(LocalDateTime.now());
            amendment.setProcessedBy("SYSTEM_AUTO_APPROVAL");
            amendment.setProcessingComments("Auto-approved: Amendment under threshold and no consent required");
        }

        return amendmentRepository.save(amendment);
    }

    @Transactional
    public Amendment updateAmendment(Long amendmentId, Amendment updatedAmendment) {
        Amendment existingAmendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new AmendmentNotFoundException("Amendment not found with id: " + amendmentId));

        // Only allow updates if amendment is still in draft status
        if (existingAmendment.getStatus() != GuaranteeStatus.DRAFT) {
            throw new IllegalArgumentException("Cannot update amendment that is no longer in draft status");
        }

        // Update modifiable fields
        existingAmendment.setDescription(updatedAmendment.getDescription());
        existingAmendment.setReason(updatedAmendment.getReason());
        existingAmendment.setChangesJson(updatedAmendment.getChangesJson());
        
        // Update amendment type if provided
        if (updatedAmendment.getAmendmentType() != null) {
            existingAmendment.setAmendmentType(updatedAmendment.getAmendmentType());
            // The setAmendmentType method will automatically update requiresConsent
        }
        
        // Re-evaluate consent requirement if explicitly provided
        if (updatedAmendment.getRequiresConsent() != null) {
            existingAmendment.setRequiresConsent(updatedAmendment.getRequiresConsent());
        }

        return amendmentRepository.save(existingAmendment);
    }

    @Transactional
    public Amendment approveAmendment(Long amendmentId, String comments) {
        Amendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new AmendmentNotFoundException("Amendment not found with id: " + amendmentId));

        // Validate amendment can be approved
        if (amendment.getStatus() != GuaranteeStatus.SUBMITTED && amendment.getStatus() != GuaranteeStatus.DRAFT) {
            throw new IllegalArgumentException("Amendment cannot be approved in current status: " + amendment.getStatus());
        }

        // Check if consent is required but not received
        if (amendment.getRequiresConsent() && amendment.getConsentReceivedDate() == null) {
            throw new IllegalArgumentException("Cannot approve amendment: consent required but not received");
        }

        amendment.setStatus(GuaranteeStatus.APPROVED);
        amendment.setProcessedDate(LocalDateTime.now());
        amendment.setProcessedBy(getCurrentUsername()); // TODO: Get from security context
        amendment.setProcessingComments(comments);

        // Apply amendment changes to the guarantee contract (placeholder for full implementation)
        applyAmendmentToGuarantee(amendment);

        return amendmentRepository.save(amendment);
    }

    @Transactional
    public Amendment rejectAmendment(Long amendmentId, String reason) {
        Amendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new AmendmentNotFoundException("Amendment not found with id: " + amendmentId));

        amendment.setStatus(GuaranteeStatus.REJECTED);
        amendment.setProcessedDate(LocalDateTime.now());
        amendment.setProcessedBy(getCurrentUsername()); // TODO: Get from security context
        amendment.setProcessingComments("Rejected: " + reason);

        return amendmentRepository.save(amendment);
    }

    @Transactional
    public Amendment recordConsentReceived(Long amendmentId, LocalDateTime consentDate) {
        Amendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new AmendmentNotFoundException("Amendment not found with id: " + amendmentId));

        if (!amendment.getRequiresConsent()) {
            throw new IllegalArgumentException("Amendment does not require consent");
        }

        amendment.setConsentReceivedDate(consentDate);

        // Move to submitted status if consent received
        if (amendment.getStatus() == GuaranteeStatus.DRAFT) {
            amendment.setStatus(GuaranteeStatus.SUBMITTED);
        }

        return amendmentRepository.save(amendment);
    }

    @Transactional
    public void cancelAmendment(Long amendmentId) {
        Amendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new AmendmentNotFoundException("Amendment not found with id: " + amendmentId));

        if (amendment.getStatus() == GuaranteeStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot cancel approved amendment");
        }

        amendment.setStatus(GuaranteeStatus.CANCELLED);
        amendment.setProcessedDate(LocalDateTime.now());
        amendment.setProcessedBy(getCurrentUsername()); // TODO: Get from security context

        amendmentRepository.save(amendment);
    }

    /**
     * Determine if consent is required for this amendment based on business rules
     */
    private boolean determineConsentRequired(Amendment amendment) {
        // Parse changes from JSON to determine if consent is needed
        // This is a simplified implementation - in reality would parse the changesJson
        String changes = amendment.getChangesJson();
        
        // Business rules for consent requirement:
        // - Amount increases always require consent
        // - Expiry date extensions require consent
        // - Beneficiary changes require consent
        // - Text-only changes may not require consent
        
        if (changes != null) {
            String lowerChanges = changes.toLowerCase();
            return lowerChanges.contains("amount") || 
                   lowerChanges.contains("expiry") || 
                   lowerChanges.contains("beneficiary");
        }
        
        return false; // Default to no consent required
    }

    /**
     * Check if amendment is under auto-approval threshold
     */
    private boolean isUnderAutoApprovalThreshold(Amendment amendment) {
        // This would need to parse the changesJson to determine the monetary impact
        // Simplified implementation for POC
        return true; // For now, assume all are under threshold
    }

    /**
     * Apply approved amendment changes to the guarantee contract
     */
    private void applyAmendmentToGuarantee(Amendment amendment) {
        GuaranteeContract guarantee = amendment.getGuarantee();
        
        // Parse amendment changes and apply them to the guarantee
        // This is a simplified implementation - in reality would parse changesJson
        // and update the appropriate fields on the guarantee
        
        // Example: If amendment changes amount, update guarantee amount
        // guarantee.setAmount(newAmount);
        // guaranteeRepository.save(guarantee);
        
        // For POC, we'll just add a log entry
        logger.info("Applied amendment {} to guarantee {}", 
                   amendment.getAmendmentReference(), guarantee.getReference());
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
}
