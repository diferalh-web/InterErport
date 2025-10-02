package com.interexport.guarantees.controller;

import com.interexport.guarantees.entity.FxRate;
import com.interexport.guarantees.entity.enums.FxRateProvider;
import com.interexport.guarantees.service.FxRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for FX Rate operations.
 * Handles foreign exchange rate management and retrieval.
 */
@RestController
@RequestMapping("/fx-rates")
@Tag(name = "FX Rates", description = "Foreign Exchange Rate Management API")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FxRateController {

    private final FxRateService fxRateService;

    @Autowired
    public FxRateController(FxRateService fxRateService) {
        this.fxRateService = fxRateService;
    }

    /**
     * Get all FX rates with pagination
     */
    @GetMapping
    @Operation(summary = "Get all FX rates", description = "Retrieve all foreign exchange rates with pagination")
    public ResponseEntity<Page<FxRate>> getAllFxRates(Pageable pageable) {
        Page<FxRate> fxRates = fxRateService.findAll(pageable);
        return ResponseEntity.ok(fxRates);
    }

    /**
     * Get latest rate for currency pair
     */
    @GetMapping("/latest")
    @Operation(summary = "Get latest rate", description = "Get the latest exchange rate for a currency pair")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rate found"),
        @ApiResponse(responseCode = "404", description = "Rate not found")
    })
    public ResponseEntity<FxRate> getLatestRate(
            @Parameter(description = "Base currency") @RequestParam String baseCurrency,
            @Parameter(description = "Target currency") @RequestParam String targetCurrency) {
        
        Optional<FxRate> fxRate = fxRateService.getLatestRate(baseCurrency, targetCurrency);
        return fxRate.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get rates for a specific date
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "Get rates for date", description = "Get all exchange rates for a specific date")
    public ResponseEntity<List<FxRate>> getRatesForDate(
            @Parameter(description = "Date (YYYY-MM-DD)") @PathVariable LocalDate date) {
        
        List<FxRate> rates = fxRateService.findByEffectiveDate(date);
        return ResponseEntity.ok(rates);
    }

    /**
     * Get rates by provider
     */
    @GetMapping("/provider/{provider}")
    @Operation(summary = "Get rates by provider", description = "Get exchange rates from a specific provider")
    public ResponseEntity<List<FxRate>> getRatesByProvider(
            @Parameter(description = "FX rate provider") @PathVariable FxRateProvider provider) {
        
        List<FxRate> rates = fxRateService.findByProvider(provider);
        return ResponseEntity.ok(rates);
    }

    /**
     * Create new FX rate
     */
    @PostMapping
    @Operation(summary = "Create new FX rate", description = "Add a new foreign exchange rate")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "FX rate created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rate data")
    })
    public ResponseEntity<FxRate> createFxRate(@Valid @RequestBody FxRate fxRate) {
        FxRate createdRate = fxRateService.createRate(fxRate);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdRate.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(createdRate);
    }

    /**
     * Update FX rate
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update FX rate", description = "Update an existing foreign exchange rate")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "FX rate updated successfully"),
        @ApiResponse(responseCode = "404", description = "FX rate not found")
    })
    public ResponseEntity<FxRate> updateFxRate(
            @Parameter(description = "FX rate ID") @PathVariable Long id,
            @Valid @RequestBody FxRate fxRate) {
        
        FxRate updatedRate = fxRateService.updateRate(id, fxRate);
        return ResponseEntity.ok(updatedRate);
    }

    /**
     * Get FX rate by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get FX rate by ID", description = "Retrieve a specific FX rate by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "FX rate found"),
        @ApiResponse(responseCode = "404", description = "FX rate not found")
    })
    public ResponseEntity<FxRate> getFxRateById(
            @Parameter(description = "FX rate ID") @PathVariable Long id) {
        
        FxRate fxRate = fxRateService.findById(id);
        return ResponseEntity.ok(fxRate);
    }

    /**
     * Convert amount between currencies
     */
    @GetMapping("/convert")
    @Operation(summary = "Convert amount", description = "Convert amount from one currency to another")
    public ResponseEntity<Map<String, Object>> convertAmount(
            @Parameter(description = "Base currency") @RequestParam String fromCurrency,
            @Parameter(description = "Target currency") @RequestParam String toCurrency,
            @Parameter(description = "Amount to convert") @RequestParam BigDecimal amount) {
        
        BigDecimal convertedAmount = fxRateService.convertAmount(amount, fromCurrency, toCurrency);
        Optional<FxRate> rate = fxRateService.getLatestRate(fromCurrency, toCurrency);
        
        return ResponseEntity.ok(Map.of(
                "originalAmount", amount,
                "fromCurrency", fromCurrency,
                "toCurrency", toCurrency,
                "convertedAmount", convertedAmount,
                "exchangeRate", rate.map(FxRate::getRate).orElse(BigDecimal.ZERO),
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Get supported currencies
     */
    @GetMapping("/currencies")
    @Operation(summary = "Get supported currencies", description = "Get list of all supported currencies")
    public ResponseEntity<List<String>> getSupportedCurrencies() {
        List<String> currencies = fxRateService.getAllSupportedCurrencies();
        return ResponseEntity.ok(currencies);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the FX rates service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "FX Rates Module",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
