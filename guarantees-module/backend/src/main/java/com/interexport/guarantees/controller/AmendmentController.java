package com.interexport.guarantees.controller;

import com.interexport.guarantees.dto.AmendmentCreateRequest;
import com.interexport.guarantees.entity.Amendment;
import com.interexport.guarantees.entity.enums.AmendmentType;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.service.AmendmentService;
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

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Amendment Management (F5)
 * Handles amendments with and without consent (GITRAM/GITAME)
 * 
 * Implements requirements:
 * - UC5.1: Create amendment with GITRAM (with consent)
 * - UC5.2: Create amendment with GITAME (without consent) 
 * - UC5.3: Process received consent confirmation
 * - UC5.4: Auto-approve amendments under threshold
 * - UC5.5: Query amendments by guarantee with history
 */
@RestController
@RequestMapping("/guarantees/{guaranteeId}/amendments")
@Tag(name = "Amendment Management", description = "API for managing guarantee amendments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AmendmentController {

    private final AmendmentService amendmentService;

    @Autowired
    public AmendmentController(AmendmentService amendmentService) {
        this.amendmentService = amendmentService;
    }

    @Operation(summary = "Get all amendments for a guarantee")
    @GetMapping
    public ResponseEntity<Page<Amendment>> getAmendments(
            @Parameter(description = "Guarantee ID") @PathVariable Long guaranteeId,
            @Parameter(description = "Pagination and sorting information") Pageable pageable) {
        Page<Amendment> amendments = amendmentService.findByGuaranteeId(guaranteeId, pageable);
        return ResponseEntity.ok(amendments);
    }

    @Operation(summary = "Get amendment by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the amendment"),
            @ApiResponse(responseCode = "404", description = "Amendment not found")
    })
    @GetMapping("/{amendmentId}")
    public ResponseEntity<Amendment> getAmendment(
            @PathVariable Long guaranteeId,
            @PathVariable Long amendmentId) {
        Optional<Amendment> amendment = amendmentService.findById(amendmentId);
        return amendment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create new amendment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Amendment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid amendment data"),
            @ApiResponse(responseCode = "404", description = "Guarantee not found")
    })
    @PostMapping
    public ResponseEntity<Amendment> createAmendment(
            @PathVariable Long guaranteeId,
            @Valid @RequestBody AmendmentCreateRequest request) {
        
        Amendment createdAmendment = amendmentService.createAmendment(guaranteeId, request);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdAmendment.getId())
                .toUri();
                
        return ResponseEntity.created(location).body(createdAmendment);
    }

    @Operation(summary = "Update amendment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Amendment updated successfully"),
            @ApiResponse(responseCode = "404", description = "Amendment not found"),
            @ApiResponse(responseCode = "400", description = "Invalid amendment data")
    })
    @PutMapping("/{amendmentId}")
    public ResponseEntity<Amendment> updateAmendment(
            @PathVariable Long guaranteeId,
            @PathVariable Long amendmentId,
            @Valid @RequestBody Amendment amendment) {
        
        Amendment updatedAmendment = amendmentService.updateAmendment(amendmentId, amendment);
        return ResponseEntity.ok(updatedAmendment);
    }

    @Operation(summary = "Approve amendment")
    @PostMapping("/{amendmentId}/approve")
    public ResponseEntity<Amendment> approveAmendment(
            @PathVariable Long guaranteeId,
            @PathVariable Long amendmentId,
            @RequestParam(required = false) String comments) {
        
        Amendment approvedAmendment = amendmentService.approveAmendment(amendmentId, comments);
        return ResponseEntity.ok(approvedAmendment);
    }

    @Operation(summary = "Reject amendment")
    @PostMapping("/{amendmentId}/reject")
    public ResponseEntity<Amendment> rejectAmendment(
            @PathVariable Long guaranteeId,
            @PathVariable Long amendmentId,
            @RequestParam String reason) {
        
        Amendment rejectedAmendment = amendmentService.rejectAmendment(amendmentId, reason);
        return ResponseEntity.ok(rejectedAmendment);
    }

    @Operation(summary = "Record consent received for amendment")
    @PostMapping("/{amendmentId}/consent")
    public ResponseEntity<Amendment> recordConsent(
            @PathVariable Long guaranteeId,
            @PathVariable Long amendmentId,
            @RequestParam LocalDateTime consentDate) {
        
        Amendment amendment = amendmentService.recordConsentReceived(amendmentId, consentDate);
        return ResponseEntity.ok(amendment);
    }

    @Operation(summary = "Get amendments by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Amendment>> getAmendmentsByStatus(
            @PathVariable Long guaranteeId,
            @PathVariable GuaranteeStatus status) {
        
        List<Amendment> amendments = amendmentService.findByGuaranteeIdAndStatus(guaranteeId, status);
        return ResponseEntity.ok(amendments);
    }

    @Operation(summary = "Get amendments by type")
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Amendment>> getAmendmentsByType(
            @PathVariable Long guaranteeId,
            @PathVariable AmendmentType type) {
        
        List<Amendment> amendments = amendmentService.findByGuaranteeIdAndType(guaranteeId, type);
        return ResponseEntity.ok(amendments);
    }

    @Operation(summary = "Cancel amendment") 
    @DeleteMapping("/{amendmentId}")
    public ResponseEntity<Void> cancelAmendment(
            @PathVariable Long guaranteeId,
            @PathVariable Long amendmentId) {
        
        amendmentService.cancelAmendment(amendmentId);
        return ResponseEntity.noContent().build();
    }
}
