package com.interexport.guarantees.controller;

import com.interexport.guarantees.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Redis cache management
 * Provides endpoints for cache operations and monitoring
 * 
 * @author InterExport Development Team
 * @version 1.0.0-POC
 */
@RestController
@RequestMapping("/redis")
@Tag(name = "Redis Management", description = "Redis cache management and monitoring endpoints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RedisController {

    private final RedisService redisService;

    @Autowired
    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get cache statistics", description = "Get statistics about Redis cache usage")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> getCacheStats() {
        String stats = redisService.getCacheStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear dashboard cache
     */
    @PostMapping("/clear/dashboard")
    @Operation(summary = "Clear dashboard cache", description = "Clear all dashboard-related cache entries")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard cache cleared successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> clearDashboardCache() {
        redisService.clearDashboardCache();
        return ResponseEntity.ok(Map.of("message", "Dashboard cache cleared successfully"));
    }

    /**
     * Clear FX rates cache
     */
    @PostMapping("/clear/fx-rates")
    @Operation(summary = "Clear FX rates cache", description = "Clear all FX rates cache entries")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "FX rates cache cleared successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> clearFxRatesCache() {
        redisService.clearFxRatesCache();
        return ResponseEntity.ok(Map.of("message", "FX rates cache cleared successfully"));
    }

    /**
     * Clear all cache
     */
    @PostMapping("/clear/all")
    @Operation(summary = "Clear all cache", description = "Clear all Redis cache entries")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All cache cleared successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> clearAllCache() {
        redisService.clearAllCache();
        return ResponseEntity.ok(Map.of("message", "All cache cleared successfully"));
    }

    /**
     * Check if a key exists
     */
    @GetMapping("/exists/{key}")
    @Operation(summary = "Check key existence", description = "Check if a specific key exists in Redis")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Key existence checked successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> keyExists(@PathVariable String key) {
        boolean exists = redisService.exists(key);
        long ttl = redisService.getTtl(key);
        return ResponseEntity.ok(Map.of(
            "key", key,
            "exists", exists,
            "ttl", ttl
        ));
    }

    /**
     * Delete a specific key
     */
    @DeleteMapping("/key/{key}")
    @Operation(summary = "Delete key", description = "Delete a specific key from Redis")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Key deleted successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> deleteKey(@PathVariable String key) {
        redisService.delete(key);
        return ResponseEntity.ok(Map.of("message", "Key '" + key + "' deleted successfully"));
    }
}

