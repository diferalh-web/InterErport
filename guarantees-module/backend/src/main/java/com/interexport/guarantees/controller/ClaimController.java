package com.interexport.guarantees.controller;

import com.interexport.guarantees.dto.ClaimCreateRequest;
import com.interexport.guarantees.entity.Claim;
import com.interexport.guarantees.entity.enums.ClaimStatus;
import com.interexport.guarantees.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Claim Management (F6)
 * Handles claim requests, payments, and rejections (GITCRQ, GITCRJ, GITSET)
 * 
 * Implements requirements:
 * - UC6.1: Create claim with validation against guarantee amount
 * - UC6.2: Process claim with approve/reject decision  
 * - UC6.3: Calculate payment with FX if needed
 * - UC6.4: Record settlement with payment reference
 * - UC6.5: Query claims with filters and pagination
 */
@RestController
@RequestMapping("/guarantees/{guaranteeId}/claims")
@Tag(name = "Claim Management", description = "API for managing guarantee claims")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ClaimController {

    private final ClaimService claimService;

    @Autowired
    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @Operation(summary = "Get all claims for a guarantee")
    @GetMapping
    public ResponseEntity<Page<Claim>> getClaims(
            @Parameter(description = "Guarantee ID") @PathVariable Long guaranteeId,
            @Parameter(description = "Pagination and sorting information") Pageable pageable) {
        Page<Claim> claims = claimService.findByGuaranteeId(guaranteeId, pageable);
        return ResponseEntity.ok(claims);
    }

    @Operation(summary = "Get claim by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the claim"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    @GetMapping("/{claimId}")
    public ResponseEntity<Claim> getClaim(
            @PathVariable Long guaranteeId,
            @PathVariable Long claimId) {
        Optional<Claim> claim = claimService.findById(claimId);
        return claim.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create new claim")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Claim created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid claim data"),
            @ApiResponse(responseCode = "404", description = "Guarantee not found"),
            @ApiResponse(responseCode = "409", description = "Claim amount exceeds guarantee amount")
    })
    @PostMapping
    public ResponseEntity<Claim> createClaim(
            @PathVariable Long guaranteeId,
            @Valid @RequestBody ClaimCreateRequest request) {
        
        Claim createdClaim = claimService.createClaimFromRequest(guaranteeId, request);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdClaim.getId())
                .toUri();
                
        return ResponseEntity.created(location).body(createdClaim);
    }

    @Operation(summary = "Update claim")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Claim updated successfully"),
            @ApiResponse(responseCode = "404", description = "Claim not found"),
            @ApiResponse(responseCode = "400", description = "Invalid claim data or claim cannot be updated")
    })
    @PutMapping("/{claimId}")
    public ResponseEntity<Claim> updateClaim(
            @PathVariable Long guaranteeId,
            @PathVariable Long claimId,
            @Valid @RequestBody Claim claim) {
        
        Claim updatedClaim = claimService.updateClaim(claimId, claim);
        return ResponseEntity.ok(updatedClaim);
    }

    @Operation(summary = "Approve claim for payment")
    @PostMapping("/{claimId}/approve")
    public ResponseEntity<Claim> approveClaim(
            @PathVariable Long guaranteeId,
            @PathVariable Long claimId,
            @RequestParam(required = false) String comments) {
        
        Claim approvedClaim = claimService.approveClaim(claimId, comments);
        return ResponseEntity.ok(approvedClaim);
    }

    @Operation(summary = "Reject claim")
    @PostMapping("/{claimId}/reject")
    public ResponseEntity<Claim> rejectClaim(
            @PathVariable Long guaranteeId,
            @PathVariable Long claimId,
            @RequestParam String reason) {
        
        Claim rejectedClaim = claimService.rejectClaim(claimId, reason);
        return ResponseEntity.ok(rejectedClaim);
    }

    @Operation(summary = "Settle claim with payment")
    @PostMapping("/{claimId}/settle")
    public ResponseEntity<Claim> settleClaim(
            @PathVariable Long guaranteeId,
            @PathVariable Long claimId,
            @RequestParam BigDecimal paymentAmount,
            @RequestParam String paymentReference,
            @RequestParam(required = false) String paymentCurrency) {
        
        Claim settledClaim = claimService.settleClaim(claimId, paymentAmount, paymentReference, paymentCurrency);
        return ResponseEntity.ok(settledClaim);
    }

    @Operation(summary = "Request additional documents for claim")
    @PostMapping("/{claimId}/request-documents")
    public ResponseEntity<Claim> requestDocuments(
            @PathVariable Long guaranteeId,
            @PathVariable Long claimId,
            @RequestParam String missingDocuments,
            @RequestParam LocalDate deadline) {
        
        Claim claim = claimService.requestAdditionalDocuments(claimId, missingDocuments, deadline);
        return ResponseEntity.ok(claim);
    }

    @Operation(summary = "Get claims by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Claim>> getClaimsByStatus(
            @PathVariable Long guaranteeId,
            @PathVariable ClaimStatus status) {
        
        List<Claim> claims = claimService.findByGuaranteeIdAndStatus(guaranteeId, status);
        return ResponseEntity.ok(claims);
    }

    @Operation(summary = "Get pending claims (requiring action)")
    @GetMapping("/pending")
    public ResponseEntity<List<Claim>> getPendingClaims(
            @PathVariable Long guaranteeId) {
        
        List<Claim> pendingClaims = claimService.findPendingClaimsByGuaranteeId(guaranteeId);
        return ResponseEntity.ok(pendingClaims);
    }

    @Operation(summary = "Get claims by amount range")
    @GetMapping("/amount-range")
    public ResponseEntity<List<Claim>> getClaimsByAmountRange(
            @PathVariable Long guaranteeId,
            @RequestParam BigDecimal minAmount,
            @RequestParam BigDecimal maxAmount) {
        
        List<Claim> claims = claimService.findByGuaranteeIdAndAmountRange(guaranteeId, minAmount, maxAmount);
        return ResponseEntity.ok(claims);
    }

    @Operation(summary = "Get claims summary for guarantee")
    @GetMapping("/summary")
    public ResponseEntity<ClaimService.ClaimSummary> getClaimsSummary(
            @PathVariable Long guaranteeId) {
        
        ClaimService.ClaimSummary summary = claimService.getClaimsSummary(guaranteeId);
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Cancel claim")
    @DeleteMapping("/{claimId}")
    public ResponseEntity<Void> cancelClaim(
            @PathVariable Long guaranteeId,
            @PathVariable Long claimId,
            @RequestParam String reason) {
        
        claimService.cancelClaim(claimId, reason);
        return ResponseEntity.noContent().build();
    }
}
