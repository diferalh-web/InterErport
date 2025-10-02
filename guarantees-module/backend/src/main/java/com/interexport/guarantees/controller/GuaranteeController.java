package com.interexport.guarantees.controller;

import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.service.GuaranteeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * REST Controller for Guarantee Contract operations.
 * Implements API requirements from technical specifications.
 */
@RestController
@RequestMapping("/guarantees")
@Tag(name = "Guarantees", description = "Guarantee Contract Management API")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GuaranteeController {

    private final GuaranteeService guaranteeService;

    @Autowired
    public GuaranteeController(GuaranteeService guaranteeService) {
        this.guaranteeService = guaranteeService;
    }

    /**
     * Create new guarantee
     * POST /guarantees
     */
    @PostMapping
    @Operation(summary = "Create new guarantee", description = "Create a new guarantee contract")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Guarantee created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid guarantee data"),
        @ApiResponse(responseCode = "409", description = "Guarantee reference already exists")
    })
    public ResponseEntity<GuaranteeContract> createGuarantee(
            @Valid @RequestBody GuaranteeContract guarantee) {
        
        GuaranteeContract createdGuarantee = guaranteeService.createGuarantee(guarantee);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdGuarantee.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(createdGuarantee);
    }

    /**
     * Get guarantee by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get guarantee by ID", description = "Retrieve guarantee by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Guarantee found"),
        @ApiResponse(responseCode = "404", description = "Guarantee not found")
    })
    public ResponseEntity<GuaranteeContract> getGuarantee(
            @Parameter(description = "Guarantee ID") @PathVariable Long id) {
        
        GuaranteeContract guarantee = guaranteeService.findById(id);
        return ResponseEntity.ok(guarantee);
    }

    /**
     * Get guarantee by reference
     */
    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get guarantee by reference", description = "Retrieve guarantee by its business reference")
    public ResponseEntity<GuaranteeContract> getGuaranteeByReference(
            @Parameter(description = "Guarantee reference") @PathVariable String reference) {
        
        Optional<GuaranteeContract> guarantee = guaranteeService.findByReference(reference);
        return guarantee.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update guarantee
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update guarantee", description = "Update an existing guarantee contract")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Guarantee updated successfully"),
        @ApiResponse(responseCode = "404", description = "Guarantee not found"),
        @ApiResponse(responseCode = "400", description = "Invalid guarantee state or data")
    })
    public ResponseEntity<GuaranteeContract> updateGuarantee(
            @Parameter(description = "Guarantee ID") @PathVariable Long id,
            @Valid @RequestBody GuaranteeContract guarantee) {
        
        GuaranteeContract updatedGuarantee = guaranteeService.updateGuarantee(id, guarantee);
        return ResponseEntity.ok(updatedGuarantee);
    }

    /**
     * Search guarantees with filters and pagination
     */
    @GetMapping
    @Operation(summary = "Search guarantees", description = "Search guarantees with filters and pagination")
    public ResponseEntity<Page<GuaranteeContract>> searchGuarantees(
            @Parameter(description = "Guarantee reference filter") @RequestParam(required = false) String reference,
            @Parameter(description = "Status filter") @RequestParam(required = false) GuaranteeStatus status,
            @Parameter(description = "Type filter") @RequestParam(required = false) GuaranteeType guaranteeType,
            @Parameter(description = "Applicant ID filter") @RequestParam(required = false) Long applicantId,
            @Parameter(description = "Currency filter") @RequestParam(required = false) String currency,
            @Parameter(description = "From date filter") @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "To date filter") @RequestParam(required = false) LocalDate toDate,
            Pageable pageable) {
        
        Page<GuaranteeContract> guarantees = guaranteeService.searchGuarantees(
                reference, status, guaranteeType, applicantId, currency, fromDate, toDate, pageable);
        
        return ResponseEntity.ok(guarantees);
    }

    /**
     * Submit guarantee for approval
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit for approval", description = "Submit guarantee for approval")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Guarantee submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid guarantee state"),
        @ApiResponse(responseCode = "404", description = "Guarantee not found")
    })
    public ResponseEntity<GuaranteeContract> submitGuarantee(
            @Parameter(description = "Guarantee ID") @PathVariable Long id) {
        
        GuaranteeContract submittedGuarantee = guaranteeService.submitForApproval(id);
        return ResponseEntity.ok(submittedGuarantee);
    }

    /**
     * Approve guarantee
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve guarantee", description = "Approve a submitted guarantee")
    public ResponseEntity<GuaranteeContract> approveGuarantee(
            @Parameter(description = "Guarantee ID") @PathVariable Long id,
            @RequestParam String approvedBy) {
        
        GuaranteeContract approvedGuarantee = guaranteeService.approveGuarantee(id, approvedBy);
        return ResponseEntity.ok(approvedGuarantee);
    }

    /**
     * Reject guarantee
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject guarantee", description = "Reject a submitted guarantee")
    public ResponseEntity<GuaranteeContract> rejectGuarantee(
            @Parameter(description = "Guarantee ID") @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam String reason) {
        
        GuaranteeContract rejectedGuarantee = guaranteeService.rejectGuarantee(id, rejectedBy, reason);
        return ResponseEntity.ok(rejectedGuarantee);
    }

    /**
     * Cancel guarantee
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel guarantee", description = "Cancel a guarantee")
    public ResponseEntity<GuaranteeContract> cancelGuarantee(
            @Parameter(description = "Guarantee ID") @PathVariable Long id,
            @RequestParam String cancelledBy,
            @RequestParam String reason) {
        
        GuaranteeContract cancelledGuarantee = guaranteeService.cancelGuarantee(id, cancelledBy, reason);
        return ResponseEntity.ok(cancelledGuarantee);
    }

    /**
     * Get guarantee with full details
     */
    @GetMapping("/{id}/details")
    @Operation(summary = "Get guarantee details", description = "Get guarantee with amendments, claims, and fees")
    public ResponseEntity<GuaranteeContract> getGuaranteeDetails(
            @Parameter(description = "Guarantee ID") @PathVariable Long id) {
        
        Optional<GuaranteeContract> guarantee = guaranteeService.findByIdWithDetails(id);
        return guarantee.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get guarantees expiring soon
     */
    @GetMapping("/expiring")
    @Operation(summary = "Get expiring guarantees", description = "Get guarantees expiring within specified days")
    public ResponseEntity<List<GuaranteeContract>> getExpiringSoon(
            @Parameter(description = "Days ahead to check") @RequestParam(defaultValue = "30") int daysAhead) {
        
        List<GuaranteeContract> expiringGuarantees = guaranteeService.findExpiringSoon(daysAhead);
        return ResponseEntity.ok(expiringGuarantees);
    }

    /**
     * Get outstanding amounts summary
     */
    @GetMapping("/outstanding-amounts")
    @Operation(summary = "Get outstanding amounts", description = "Get total outstanding amounts by currency")
    public ResponseEntity<Map<String, Object>> getOutstandingAmounts() {
        
        List<Object[]> amountsByCurrency = guaranteeService.getTotalOutstandingAmountByCurrency();
        BigDecimal totalInBase = guaranteeService.getTotalOutstandingAmountInBaseCurrency();
        
        return ResponseEntity.ok(Map.of(
                "byCurrency", amountsByCurrency,
                "totalInBaseCurrency", totalInBase
        ));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the guarantees service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Guarantees Module",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }


    @GetMapping("/test-simple")
    @Operation(summary = "Simple test endpoint")
    public ResponseEntity<Map<String, String>> testSimple() {
        return ResponseEntity.ok(Map.of("message", "Test endpoint works", "status", "OK"));
    }
}
