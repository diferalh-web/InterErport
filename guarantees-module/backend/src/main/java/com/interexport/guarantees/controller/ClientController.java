package com.interexport.guarantees.controller;

import com.interexport.guarantees.entity.Client;
import com.interexport.guarantees.service.ClientService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Client Management
 * Handles client CRUD operations and search functionality
 * 
 * Implements requirements:
 * - Client registration and management
 * - Client search and filtering
 * - Client KYC and risk management
 * - Client credit limit management
 */
@RestController
@RequestMapping("/clients")
@Tag(name = "Client Management", description = "API for managing guarantee clients and applicants")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "Get all clients with pagination")
    @GetMapping
    public ResponseEntity<Page<Client>> getAllClients(
            @Parameter(description = "Client code filter") @RequestParam(required = false) String clientCode,
            @Parameter(description = "Name filter") @RequestParam(required = false) String name,
            @Parameter(description = "Country filter") @RequestParam(required = false) String countryCode,
            @Parameter(description = "Risk rating filter") @RequestParam(required = false) String riskRating,
            @Parameter(description = "Active status filter") @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        
        // If any specific filters are provided, use criteria search
        if (clientCode != null || name != null || countryCode != null || riskRating != null || isActive != null) {
            Page<Client> clients = clientService.searchByCriteria(clientCode, name, countryCode, riskRating, isActive, pageable);
            return ResponseEntity.ok(clients);
        } else {
            // Show all clients
            Page<Client> clients = clientService.findAll(pageable);
            return ResponseEntity.ok(clients);
        }
    }

    @Operation(summary = "Get client by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the client"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Client> getClient(@PathVariable Long id) {
        Optional<Client> client = clientService.findById(id);
        return client.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get client by client code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the client"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/code/{clientCode}")
    public ResponseEntity<Client> getClientByCode(@PathVariable String clientCode) {
        Optional<Client> client = clientService.findByClientCode(clientCode);
        return client.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search clients by name")
    @GetMapping("/search")
    public ResponseEntity<Page<Client>> searchClients(
            @Parameter(description = "Search term for client name") @RequestParam String q,
            Pageable pageable) {
        
        // Use criteria search with name parameter to ensure pagination works
        Page<Client> clients = clientService.searchByCriteria(null, q, null, null, null, pageable);
        return ResponseEntity.ok(clients);
    }

    @Operation(summary = "Create a new client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Client created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid client data"),
            @ApiResponse(responseCode = "409", description = "Client code already exists")
    })
    @PostMapping
    public ResponseEntity<Client> createClient(@Valid @RequestBody Client client) {
        Client createdClient = clientService.createClient(client);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdClient.getId())
                .toUri();
                
        return ResponseEntity.created(location).body(createdClient);
    }

    @Operation(summary = "Update an existing client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client updated successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "400", description = "Invalid client data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(
            @PathVariable Long id, 
            @Valid @RequestBody Client client) {
        Client updatedClient = clientService.updateClient(id, client);
        return ResponseEntity.ok(updatedClient);
    }

    @Operation(summary = "Delete a client (soft delete - marks as inactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Client deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get clients with KYC review due")
    @GetMapping("/kyc-due")
    public ResponseEntity<List<Client>> getClientsWithKycReviewDue() {
        List<Client> clients = clientService.findClientsWithKycReviewDue();
        return ResponseEntity.ok(clients);
    }

    @Operation(summary = "Get clients with upcoming KYC review")
    @GetMapping("/kyc-upcoming")
    public ResponseEntity<List<Client>> getClientsWithUpcomingKycReview(
            @Parameter(description = "Future date to check (YYYY-MM-DD)") 
            @RequestParam(required = false) String futureDate) {
        
        LocalDate checkDate;
        if (futureDate != null && !futureDate.isEmpty()) {
            checkDate = LocalDate.parse(futureDate);
        } else {
            checkDate = LocalDate.now().plusDays(30); // Default to 30 days ahead
        }
        
        List<Client> clients = clientService.findClientsWithUpcomingKycReview(checkDate);
        return ResponseEntity.ok(clients);
    }

    @Operation(summary = "Activate a client")
    @PostMapping("/{id}/activate")
    public ResponseEntity<Client> activateClient(@PathVariable Long id) {
        Client client = clientService.activateClient(id);
        return ResponseEntity.ok(client);
    }

    @Operation(summary = "Deactivate a client")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Client> deactivateClient(@PathVariable Long id) {
        Client client = clientService.deactivateClient(id);
        return ResponseEntity.ok(client);
    }
}
