package com.interexport.guarantees.controller;

import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.GuaranteeTemplate;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.service.GuaranteeTemplateService;
import com.interexport.guarantees.service.GuaranteeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for guarantee template management
 * Implements F2 - Guarantee Texts (templates + variables)
 */
@RestController
@RequestMapping("/templates")
@Tag(name = "Guarantee Templates", description = "API for managing guarantee text templates with variables")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('USER')")
public class GuaranteeTemplateController {

    private final GuaranteeTemplateService templateService;
    private final GuaranteeService guaranteeService;

    @Autowired
    public GuaranteeTemplateController(GuaranteeTemplateService templateService,
                                     GuaranteeService guaranteeService) {
        this.templateService = templateService;
        this.guaranteeService = guaranteeService;
    }

    /**
     * Simple test endpoint to verify controller is working
     */
    @GetMapping("/test")
    @Operation(summary = "Test endpoint", description = "Simple test to verify template controller is accessible")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✅ Template Controller is working! Templates found: " + 
                                templateService.getAllActiveTemplates().size());
    }

    /**
     * Get all active templates
     */
    @GetMapping
    @Operation(summary = "Get all active guarantee templates",
               description = "Retrieve a list of all active guarantee templates ordered by type and language")
    public ResponseEntity<List<GuaranteeTemplate>> getAllActiveTemplates() {
        List<GuaranteeTemplate> templates = templateService.getAllActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Get templates by guarantee type
     */
    @GetMapping("/type/{guaranteeType}")
    @Operation(summary = "Get templates by guarantee type",
               description = "Retrieve all active templates for a specific guarantee type")
    public ResponseEntity<List<GuaranteeTemplate>> getTemplatesByType(
            @Parameter(description = "Guarantee type", example = "PERFORMANCE")
            @PathVariable GuaranteeType guaranteeType) {
        
        List<GuaranteeTemplate> templates = templateService.getTemplatesByType(guaranteeType);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get template by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID",
               description = "Retrieve a specific template by its ID")
    public ResponseEntity<GuaranteeTemplate> getTemplateById(
            @Parameter(description = "Template ID", example = "1")
            @PathVariable Long id) {
        
        // This would use a repository method to find by ID
        // For now, we'll throw an exception if not found
        throw new UnsupportedOperationException("Direct template retrieval by ID not yet implemented");
    }

    /**
     * Select appropriate template for guarantee
     * UC2.1: Select template by 22A/22D/language
     */
    @GetMapping("/select")
    @Operation(summary = "Select template for guarantee",
               description = "Select the most appropriate template based on guarantee type, language, and SWIFT fields")
    public ResponseEntity<GuaranteeTemplate> selectTemplate(
            @Parameter(description = "Guarantee type", example = "PERFORMANCE")
            @RequestParam GuaranteeType guaranteeType,
            
            @Parameter(description = "Language code", example = "EN")
            @RequestParam(defaultValue = "EN") String language,
            
            @Parameter(description = "SWIFT field 22A", example = "ISPR")
            @RequestParam(required = false) String field22A,
            
            @Parameter(description = "SWIFT field 22D", example = "IPRU")
            @RequestParam(required = false) String field22D) {

        GuaranteeTemplate template = templateService.selectTemplate(
            guaranteeType, language, field22A, field22D);
        return ResponseEntity.ok(template);
    }

    /**
     * Render template preview for guarantee
     * UC2.2: Render preview with amount and date in words
     */
    @PostMapping("/{templateId}/preview")
    @Operation(summary = "Generate template preview",
               description = "Generate a preview of the template with variable substitution for a specific guarantee")
    public ResponseEntity<GuaranteeTemplateService.GuaranteeTextPreview> renderPreview(
            @Parameter(description = "Template ID", example = "1")
            @PathVariable Long templateId,
            
            @Parameter(description = "Guarantee ID", example = "1")
            @RequestParam Long guaranteeId) {

        // Get template - this would need repository access
        // For now, we'll get the guarantee and use selection logic
        GuaranteeContract guarantee = guaranteeService.findById(guaranteeId);
        GuaranteeTemplate template = templateService.selectTemplate(
            guarantee.getGuaranteeType(), 
            guarantee.getLanguage() != null ? guarantee.getLanguage() : "EN", 
            null, null);
        
        GuaranteeTemplateService.GuaranteeTextPreview preview = 
            templateService.renderPreview(template, guarantee);
        
        return ResponseEntity.ok(preview);
    }

    /**
     * Save rendered template text to guarantee
     * UC2.5: Save final text version into the guarantee
     */
    @PostMapping("/{templateId}/apply/{guaranteeId}")
    @Operation(summary = "Apply template to guarantee",
               description = "Apply the selected template to a guarantee, generating and saving the final text")
    public ResponseEntity<GuaranteeContract> applyTemplateToGuarantee(
            @Parameter(description = "Template ID", example = "1")
            @PathVariable Long templateId,
            
            @Parameter(description = "Guarantee ID", example = "1")
            @PathVariable Long guaranteeId,
            
            @Parameter(description = "Custom variable values to override defaults")
            @RequestBody(required = false) Map<String, Object> customVariables) {

        GuaranteeContract guarantee = guaranteeService.findById(guaranteeId);
        
        // Get template using selection logic for now
        GuaranteeTemplate template = templateService.selectTemplate(
            guarantee.getGuaranteeType(), 
            guarantee.getLanguage() != null ? guarantee.getLanguage() : "EN", 
            null, null);
        
        GuaranteeContract updatedGuarantee = templateService.saveRenderedText(
            guarantee, template, customVariables);
        
        // Save the updated guarantee
        GuaranteeContract savedGuarantee = guaranteeService.updateGuarantee(
            guaranteeId, updatedGuarantee);
        
        return ResponseEntity.ok(savedGuarantee);
    }

    /**
     * Create new template
     */
    @PostMapping
    @Operation(summary = "Create new guarantee template",
               description = "Create a new guarantee text template with variable placeholders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GuaranteeTemplate> createTemplate(
            @Parameter(description = "Template data")
            @Valid @RequestBody GuaranteeTemplate template) {

        GuaranteeTemplate createdTemplate = templateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTemplate);
    }

    /**
     * Update existing template
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update guarantee template",
               description = "Update an existing guarantee template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GuaranteeTemplate> updateTemplate(
            @Parameter(description = "Template ID", example = "1")
            @PathVariable Long id,
            
            @Parameter(description = "Updated template data")
            @Valid @RequestBody GuaranteeTemplate template) {

        GuaranteeTemplate updatedTemplate = templateService.updateTemplate(id, template);
        return ResponseEntity.ok(updatedTemplate);
    }

    /**
     * Get template variables information
     */
    @GetMapping("/{templateId}/variables")
    @Operation(summary = "Get template variable information",
               description = "Get information about variables used in the template")
    public ResponseEntity<TemplateVariableInfo> getTemplateVariables(
            @Parameter(description = "Template ID", example = "1")
            @PathVariable Long templateId) {

        // This would get template by ID and analyze variables
        // For now, return a sample response
        GuaranteeTemplate template = templateService.selectTemplate(GuaranteeType.PERFORMANCE, "EN", null, null);
        
        TemplateVariableInfo info = new TemplateVariableInfo(
            template.extractVariablePlaceholders(),
            template.getRequiredVariablesList(),
            template.getOptionalVariablesList(),
            getAvailableVariableDescriptions()
        );
        
        return ResponseEntity.ok(info);
    }

    /**
     * Validate template syntax and variables
     * UC2.4: Validate mandatory variables are present
     */
    @PostMapping("/{templateId}/validate")
    @Operation(summary = "Validate template",
               description = "Validate template syntax and check for required variables")
    public ResponseEntity<TemplateValidationResult> validateTemplate(
            @Parameter(description = "Template ID", example = "1")
            @PathVariable Long templateId,
            
            @Parameter(description = "Guarantee ID for validation context", example = "1")
            @RequestParam Long guaranteeId) {

        GuaranteeContract guarantee = guaranteeService.findById(guaranteeId);
        GuaranteeTemplate template = templateService.selectTemplate(
            guarantee.getGuaranteeType(), 
            guarantee.getLanguage() != null ? guarantee.getLanguage() : "EN", 
            null, null);
        
        GuaranteeTemplateService.GuaranteeTextPreview preview = 
            templateService.renderPreview(template, guarantee);
        
        TemplateValidationResult result = new TemplateValidationResult(
            preview.isValid(),
            preview.getMissingRequired(),
            preview.getAllVariables(),
            preview.isValid() ? null : "Missing required variables: " + String.join(", ", preview.getMissingRequired())
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get available variable descriptions
     */
    private Map<String, String> getAvailableVariableDescriptions() {
        return Map.of(
            "GUARANTEE_REFERENCE", "Unique reference number for the guarantee",
            "GUARANTEE_TYPE", "Type of guarantee (PERFORMANCE, ADVANCE_PAYMENT, etc.)",
            "GUARANTEE_AMOUNT", "Numerical amount of the guarantee",
            "GUARANTEE_AMOUNT_WORDS", "Amount spelled out in words",
            "CURRENCY", "Currency code (USD, EUR, etc.)",
            "ISSUE_DATE", "Date the guarantee was issued",
            "EXPIRY_DATE", "Date the guarantee expires",
            "BENEFICIARY_NAME", "Name of the guarantee beneficiary",
            "APPLICANT_ID", "ID of the guarantee applicant",
            "IS_DOMESTIC", "Whether the guarantee is domestic (YES/NO)"
        );
    }

    /**
     * Response classes
     */
    public static class TemplateVariableInfo {
        private final List<String> allVariables;
        private final List<String> requiredVariables;
        private final List<String> optionalVariables;
        private final Map<String, String> variableDescriptions;

        public TemplateVariableInfo(List<String> allVariables, List<String> requiredVariables,
                                  List<String> optionalVariables, Map<String, String> variableDescriptions) {
            this.allVariables = allVariables;
            this.requiredVariables = requiredVariables;
            this.optionalVariables = optionalVariables;
            this.variableDescriptions = variableDescriptions;
        }

        public List<String> getAllVariables() { return allVariables; }
        public List<String> getRequiredVariables() { return requiredVariables; }
        public List<String> getOptionalVariables() { return optionalVariables; }
        public Map<String, String> getVariableDescriptions() { return variableDescriptions; }
    }

    public static class TemplateValidationResult {
        private final boolean isValid;
        private final List<String> missingRequired;
        private final List<String> allVariables;
        private final String errorMessage;

        public TemplateValidationResult(boolean isValid, List<String> missingRequired,
                                      List<String> allVariables, String errorMessage) {
            this.isValid = isValid;
            this.missingRequired = missingRequired;
            this.allVariables = allVariables;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return isValid; }
        public List<String> getMissingRequired() { return missingRequired; }
        public List<String> getAllVariables() { return allVariables; }
        public String getErrorMessage() { return errorMessage; }
    }
}
