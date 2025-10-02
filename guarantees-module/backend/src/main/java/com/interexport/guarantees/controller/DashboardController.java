package com.interexport.guarantees.controller;

import com.interexport.guarantees.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST Controller for Dashboard metrics
 * Provides aggregated data for dashboard visualization
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Dashboard metrics and analytics endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get dashboard summary metrics
     */
    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Get total amounts and counts for guarantees, claims, and amendments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get monthly statistics for chart display
     */
    @GetMapping("/monthly-stats")
    @Operation(summary = "Get monthly statistics", description = "Get monthly breakdown of guarantees, claims, and amendments for chart display")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Monthly stats retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getMonthlyStatistics(
            @Parameter(description = "Number of months to look back") 
            @RequestParam(defaultValue = "12") int monthsBack) {
        Map<String, Object> monthlyStats = dashboardService.getMonthlyStatistics(monthsBack);
        return ResponseEntity.ok(monthlyStats);
    }

    /**
     * Get detailed metrics by currency
     */
    @GetMapping("/metrics-by-currency")
    @Operation(summary = "Get metrics by currency", description = "Get total amounts broken down by currency")
    public ResponseEntity<Map<String, Object>> getMetricsByCurrency() {
        Map<String, Object> metricsByCurrency = dashboardService.getMetricsByCurrency();
        return ResponseEntity.ok(metricsByCurrency);
    }

    /**
     * Get activity trend data
     */
    @GetMapping("/activity-trend")
    @Operation(summary = "Get activity trend", description = "Get daily activity trend for the last 30 days")
    public ResponseEntity<Map<String, Object>> getActivityTrend(
            @Parameter(description = "Number of days to look back") 
            @RequestParam(defaultValue = "30") int daysBack) {
        Map<String, Object> activityTrend = dashboardService.getActivityTrend(daysBack);
        return ResponseEntity.ok(activityTrend);
    }
}




