package com.interexport.guarantees.controller.cqrs;

import com.interexport.guarantees.cqrs.command.CreateGuaranteeCommand;
import com.interexport.guarantees.cqrs.command.GuaranteeCommandHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Guarantee Commands (Write Side)
 * Handles all write operations
 */
@RestController
@RequestMapping("/api/v1/cqrs/commands/guarantees")
@Tag(name = "Guarantee Commands", description = "CQRS Command endpoints for guarantee operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GuaranteeCommandController {
    
    private final GuaranteeCommandHandler commandHandler;
    
    @Autowired
    public GuaranteeCommandController(GuaranteeCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }
    
    @PostMapping
    @Operation(summary = "Create a new guarantee", description = "Creates a new guarantee and publishes events")
    public ResponseEntity<Map<String, Object>> createGuarantee(@RequestBody CreateGuaranteeCommand command) {
        try {
            String guaranteeId = commandHandler.handle(command);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("guaranteeId", guaranteeId);
            response.put("message", "Guarantee created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
