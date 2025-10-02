package com.interexport.guarantees.controller.cqrs;

import com.interexport.guarantees.cqrs.query.GuaranteeQueryHandler;
import com.interexport.guarantees.cqrs.query.GuaranteeSummaryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Guarantee Queries (Read Side)
 * Handles all read operations with optimized queries
 */
@RestController
@RequestMapping("/api/v1/cqrs/queries/guarantees")
@Tag(name = "Guarantee Queries", description = "CQRS Query endpoints for guarantee read operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GuaranteeQueryController {
    
    private final GuaranteeQueryHandler queryHandler;
    
    @Autowired
    public GuaranteeQueryController(GuaranteeQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }
    
    @GetMapping
    @Operation(summary = "Get all guarantee summaries", description = "Returns all guarantee summaries from query database")
    public ResponseEntity<List<GuaranteeSummaryView>> getAllGuarantees() {
        List<GuaranteeSummaryView> guarantees = queryHandler.getAllGuaranteeSummaries();
        return ResponseEntity.ok(guarantees);
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get guarantees by status", description = "Returns guarantees filtered by status")
    public ResponseEntity<List<GuaranteeSummaryView>> getGuaranteesByStatus(@PathVariable String status) {
        List<GuaranteeSummaryView> guarantees = queryHandler.getGuaranteeSummariesByStatus(status);
        return ResponseEntity.ok(guarantees);
    }
    
    @GetMapping("/expiring")
    @Operation(summary = "Get expiring guarantees", description = "Returns guarantees expiring in the next 30 days")
    public ResponseEntity<List<GuaranteeSummaryView>> getExpiringGuarantees() {
        List<GuaranteeSummaryView> guarantees = queryHandler.getExpiringGuarantees();
        return ResponseEntity.ok(guarantees);
    }
    
    @GetMapping("/currency/{currency}")
    @Operation(summary = "Get guarantees by currency", description = "Returns guarantees filtered by currency")
    public ResponseEntity<List<GuaranteeSummaryView>> getGuaranteesByCurrency(@PathVariable String currency) {
        List<GuaranteeSummaryView> guarantees = queryHandler.getGuaranteesByCurrency(currency);
        return ResponseEntity.ok(guarantees);
    }
    
    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get dashboard summary", description = "Returns dashboard summary statistics")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        // This would typically call a dashboard service
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGuarantees", queryHandler.getAllGuaranteeSummaries().size());
        summary.put("activeGuarantees", queryHandler.getGuaranteeSummariesByStatus("ACTIVE").size());
        summary.put("expiringGuarantees", queryHandler.getExpiringGuarantees().size());
        summary.put("timestamp", LocalDate.now());
        
        return ResponseEntity.ok(summary);
    }
}
