package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.GuaranteeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Guarantee Template entity for F2 - Guarantee Texts (templates + variables)
 * Stores predefined text templates with variable placeholders for guarantee generation
 */
@Entity
@Table(name = "guarantee_templates", indexes = {
    @Index(name = "idx_template_type", columnList = "guaranteeType"),
    @Index(name = "idx_template_language", columnList = "language"),
    @Index(name = "idx_template_active", columnList = "isActive")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_template_type_lang", 
        columnNames = {"guaranteeType", "language", "version"})
})
public class GuaranteeTemplate extends BaseEntity {

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name cannot exceed 255 characters")
    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "guarantee_type", nullable = false, length = 20)
    @NotNull(message = "Guarantee type is required")
    private GuaranteeType guaranteeType;

    @NotBlank(message = "Language is required")
    @Size(min = 2, max = 5, message = "Language must be 2-5 characters (ISO 639-1)")
    @Column(name = "language", nullable = false, length = 5)
    private String language = "EN";

    @Column(name = "version", nullable = false)
    private Long version = 1L;

    @Column(name = "template_text", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Template text is required")
    private String templateText;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    @Column(name = "is_domestic")
    private Boolean isDomestic; // null = applicable to both

    @Column(name = "required_variables", columnDefinition = "TEXT")
    private String requiredVariables; // JSON array of required variable names

    @Column(name = "optional_variables", columnDefinition = "TEXT")  
    private String optionalVariables; // JSON array of optional variable names

    @Column(name = "usage_notes", columnDefinition = "TEXT")
    private String usageNotes;

    @Column(name = "effective_from")
    private java.time.LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private java.time.LocalDate effectiveTo;

    // Constructors
    public GuaranteeTemplate() {}

    public GuaranteeTemplate(String templateName, GuaranteeType guaranteeType, String language, String templateText) {
        this.templateName = templateName;
        this.guaranteeType = guaranteeType;
        this.language = language.toUpperCase();
        this.templateText = templateText;
        this.effectiveFrom = java.time.LocalDate.now();
    }

    // Getters and Setters
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public GuaranteeType getGuaranteeType() { return guaranteeType; }
    public void setGuaranteeType(GuaranteeType guaranteeType) { this.guaranteeType = guaranteeType; }

    public String getName() { return templateName; }
    public void setName(String name) { this.templateName = name; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language != null ? language.toUpperCase() : null; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Boolean getIsDomestic() { return isDomestic; }
    public void setIsDomestic(Boolean isDomestic) { this.isDomestic = isDomestic; }

    public String getTemplateContent() { return templateText; }
    public void setTemplateContent(String templateContent) { this.templateText = templateContent; }

        public Long getTemplateVersion() { return version; }
        public void setTemplateVersion(Long version) { this.version = version; }

    public String getTemplateText() { return templateText; }
    public void setTemplateText(String templateText) { this.templateText = templateText; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public String getRequiredVariables() { return requiredVariables; }
    public void setRequiredVariables(String requiredVariables) { this.requiredVariables = requiredVariables; }

    public String getOptionalVariables() { return optionalVariables; }
    public void setOptionalVariables(String optionalVariables) { this.optionalVariables = optionalVariables; }

    public String getUsageNotes() { return usageNotes; }
    public void setUsageNotes(String usageNotes) { this.usageNotes = usageNotes; }

    public java.time.LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(java.time.LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public java.time.LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(java.time.LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    // Helper methods
    
    /**
     * Check if template is valid for the given date
     */
    public boolean isValidForDate(java.time.LocalDate date) {
        boolean afterStart = effectiveFrom == null || !date.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);
        return isActive && afterStart && beforeEnd;
    }

    /**
     * Get template display name with type and language
     */
    public String getDisplayName() {
        return String.format("%s (%s - %s) v%d", 
                templateName, guaranteeType.name(), language, version);
    }

    /**
     * Parse required variables from JSON string
     */
    public List<String> getRequiredVariablesList() {
        if (requiredVariables == null || requiredVariables.trim().isEmpty()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(requiredVariables, mapper.getTypeFactory()
                .constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Parse optional variables from JSON string  
     */
    public List<String> getOptionalVariablesList() {
        if (optionalVariables == null || optionalVariables.trim().isEmpty()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(optionalVariables, mapper.getTypeFactory()
                .constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Set required variables as JSON array
     */
    public void setRequiredVariablesList(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            this.requiredVariables = null;
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.requiredVariables = mapper.writeValueAsString(variables);
        } catch (Exception e) {
            this.requiredVariables = null;
        }
    }

    /**
     * Set optional variables as JSON array
     */
    public void setOptionalVariablesList(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            this.optionalVariables = null;
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.optionalVariables = mapper.writeValueAsString(variables);
        } catch (Exception e) {
            this.optionalVariables = null;
        }
    }

    /**
     * Extract variable placeholders from template text
     * Variables are in format {{VARIABLE_NAME}}
     */
    public List<String> extractVariablePlaceholders() {
        if (templateText == null) return List.of();
        
        List<String> variables = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([A-Z_][A-Z0-9_]*)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(templateText);
        
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (!variables.contains(variable)) {
                variables.add(variable);
            }
        }
        
        return variables;
    }

    @Override
    public String toString() {
        return String.format("GuaranteeTemplate{id=%d, name='%s', type=%s, language='%s', version=%d, active=%s}", 
                getId(), templateName, guaranteeType, language, version, isActive);
    }
}
