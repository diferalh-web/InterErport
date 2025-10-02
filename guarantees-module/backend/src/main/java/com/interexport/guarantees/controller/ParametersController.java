package com.interexport.guarantees.controller;

import com.interexport.guarantees.entity.Account;
import com.interexport.guarantees.entity.Bank;
import com.interexport.guarantees.entity.CommissionParameter;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.service.ParametersService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Parameters Module (F10)
 * Manages banks, accounts, commission parameters, and GL mapping
 * 
 * Implements requirements:
 * - UC10.1: Create and manage correspondent bank configurations
 * - UC10.2: Configure liability accounts with GL codes
 * - UC10.3: Set commission rates by guarantee type and currency
 * - UC10.4: Map accounts to accounting GL codes
 * - UC10.5: Query parameters with filters and validation
 */
@RestController
@RequestMapping("/parameters")
@Tag(name = "Parameters Management", description = "API for managing system parameters (banks, accounts, commissions, GL mapping)")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ParametersController {

    private final ParametersService parametersService;

    @Autowired
    public ParametersController(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    // === BANK MANAGEMENT ===

    @Operation(summary = "Get all banks")
    @GetMapping("/banks")
    public ResponseEntity<Page<Bank>> getAllBanks(
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Pagination information") Pageable pageable) {
        Page<Bank> banks = parametersService.searchBanks(search, pageable);
        return ResponseEntity.ok(banks);
    }

    @Operation(summary = "Get bank by ID")
    @GetMapping("/banks/{id}")
    public ResponseEntity<Bank> getBank(@PathVariable Long id) {
        Optional<Bank> bank = parametersService.findBankById(id);
        return bank.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create new bank")
    @PostMapping("/banks")
    public ResponseEntity<Bank> createBank(@Valid @RequestBody Bank bank) {
        Bank createdBank = parametersService.createBank(bank);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBank.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdBank);
    }

    @Operation(summary = "Update bank")
    @PutMapping("/banks/{id}")
    public ResponseEntity<Bank> updateBank(@PathVariable Long id, @Valid @RequestBody Bank bank) {
        Bank updatedBank = parametersService.updateBank(id, bank);
        return ResponseEntity.ok(updatedBank);
    }

    @Operation(summary = "Delete bank")
    @DeleteMapping("/banks/{id}")
    public ResponseEntity<Void> deleteBank(@PathVariable Long id) {
        parametersService.deleteBank(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get correspondent banks")
    @GetMapping("/banks/correspondent")
    public ResponseEntity<List<Bank>> getCorrespondentBanks() {
        List<Bank> banks = parametersService.getCorrespondentBanks();
        return ResponseEntity.ok(banks);
    }

    @Operation(summary = "Get banks by country")
    @GetMapping("/banks/country/{countryCode}")
    public ResponseEntity<List<Bank>> getBanksByCountry(@PathVariable String countryCode) {
        List<Bank> banks = parametersService.getBanksByCountry(countryCode);
        return ResponseEntity.ok(banks);
    }

    // === ACCOUNT MANAGEMENT ===

    @Operation(summary = "Get all accounts")
    @GetMapping("/accounts")
    public ResponseEntity<Page<Account>> getAllAccounts(
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Currency filter") @RequestParam(required = false) String currency,
            @Parameter(description = "Pagination information") Pageable pageable) {
        Page<Account> accounts = parametersService.searchAccounts(search, currency, pageable);
        return ResponseEntity.ok(accounts);
    }

    @Operation(summary = "Get account by ID")
    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        Optional<Account> account = parametersService.findAccountById(id);
        return account.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create new account")
    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody Account account) {
        Account createdAccount = parametersService.createAccount(account);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdAccount.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdAccount);
    }

    @Operation(summary = "Update account")
    @PutMapping("/accounts/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @Valid @RequestBody Account account) {
        Account updatedAccount = parametersService.updateAccount(id, account);
        return ResponseEntity.ok(updatedAccount);
    }

    @Operation(summary = "Delete account")
    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        parametersService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get accounts by bank")
    @GetMapping("/banks/{bankId}/accounts")
    public ResponseEntity<List<Account>> getAccountsByBank(@PathVariable Long bankId) {
        List<Account> accounts = parametersService.getAccountsByBank(bankId);
        return ResponseEntity.ok(accounts);
    }

    @Operation(summary = "Get accounts by currency")
    @GetMapping("/accounts/currency/{currency}")
    public ResponseEntity<List<Account>> getAccountsByCurrency(@PathVariable String currency) {
        List<Account> accounts = parametersService.getAccountsByCurrency(currency);
        return ResponseEntity.ok(accounts);
    }

    @Operation(summary = "Get liability accounts by currency")
    @GetMapping("/accounts/liability/{currency}")
    public ResponseEntity<List<Account>> getLiabilityAccounts(@PathVariable String currency) {
        List<Account> accounts = parametersService.getLiabilityAccountsByCurrency(currency);
        return ResponseEntity.ok(accounts);
    }

    // === COMMISSION PARAMETERS ===

    @Operation(summary = "Get all commission parameters")
    @GetMapping("/commissions")
    public ResponseEntity<Page<CommissionParameter>> getAllCommissionParameters(
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Guarantee type filter") @RequestParam(required = false) GuaranteeType guaranteeType,
            @Parameter(description = "Currency filter") @RequestParam(required = false) String currency,
            @Parameter(description = "Pagination information") Pageable pageable) {
        Page<CommissionParameter> parameters = parametersService.searchCommissionParameters(search, guaranteeType, currency, pageable);
        return ResponseEntity.ok(parameters);
    }

    @Operation(summary = "Get commission parameter by ID")
    @GetMapping("/commissions/{id}")
    public ResponseEntity<CommissionParameter> getCommissionParameter(@PathVariable Long id) {
        Optional<CommissionParameter> parameter = parametersService.findCommissionParameterById(id);
        return parameter.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create new commission parameter")
    @PostMapping("/commissions")
    public ResponseEntity<CommissionParameter> createCommissionParameter(@Valid @RequestBody CommissionParameter parameter) {
        CommissionParameter createdParameter = parametersService.createCommissionParameter(parameter);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdParameter.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdParameter);
    }

    @Operation(summary = "Update commission parameter")
    @PutMapping("/commissions/{id}")
    public ResponseEntity<CommissionParameter> updateCommissionParameter(@PathVariable Long id, @Valid @RequestBody CommissionParameter parameter) {
        CommissionParameter updatedParameter = parametersService.updateCommissionParameter(id, parameter);
        return ResponseEntity.ok(updatedParameter);
    }

    @Operation(summary = "Delete commission parameter")
    @DeleteMapping("/commissions/{id}")
    public ResponseEntity<Void> deleteCommissionParameter(@PathVariable Long id) {
        parametersService.deleteCommissionParameter(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get commission parameter for guarantee criteria")
    @GetMapping("/commissions/lookup")
    public ResponseEntity<CommissionParameter> getCommissionParameterForCriteria(
            @RequestParam GuaranteeType guaranteeType,
            @RequestParam String currency,
            @RequestParam Boolean isDomestic,
            @RequestParam(defaultValue = "STANDARD") String clientSegment) {
        Optional<CommissionParameter> parameter = parametersService.findCommissionParameterForCriteria(
                guaranteeType, currency, isDomestic, clientSegment);
        return parameter.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // === UTILITY ENDPOINTS ===

    @Operation(summary = "Get parameters summary")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getParametersSummary() {
        Map<String, Object> summary = parametersService.getParametersSummary();
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get all client segments")
    @GetMapping("/client-segments")
    public ResponseEntity<List<String>> getClientSegments() {
        List<String> segments = parametersService.getAllClientSegments();
        return ResponseEntity.ok(segments);
    }

    @Operation(summary = "Get currencies with parameters")
    @GetMapping("/currencies")
    public ResponseEntity<List<String>> getCurrenciesWithParameters() {
        List<String> currencies = parametersService.getCurrenciesWithParameters();
        return ResponseEntity.ok(currencies);
    }

    @Operation(summary = "Validate parameters configuration")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateParametersConfiguration() {
        Map<String, Object> validation = parametersService.validateConfiguration();
        return ResponseEntity.ok(validation);
    }
}
