package com.interexport.guarantees.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validation Service for F12 Data Migration
 * Validates imported records according to business rules and data constraints
 */
@Service
@Slf4j
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    @Autowired
    private ObjectMapper objectMapper;

    // Common validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[\\+]?[1-9]?[0-9]{7,15}$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

    /**
     * Validate a record against validation rules
     */
    public void validateRecord(Map<String, Object> record, String validationRulesJson, String targetEntity) throws Exception {
        Map<String, Object> validationRules = parseValidationRules(validationRulesJson);
        List<String> errors = new ArrayList<>();

        // Apply general validation rules
        applyGeneralValidation(record, validationRules, errors);

        // Apply entity-specific validation
        switch (targetEntity.toUpperCase()) {
            case "GUARANTEE":
                applyGuaranteeValidation(record, errors);
                break;
            case "CLIENT":
                applyClientValidation(record, errors);
                break;
            case "COMMISSION":
                applyCommissionValidation(record, errors);
                break;
        }

        // Throw exception if validation errors found
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + String.join("; ", errors));
        }
    }

    /**
     * Parse validation rules from JSON
     */
    private Map<String, Object> parseValidationRules(String validationRulesJson) {
        try {
            if (validationRulesJson == null || validationRulesJson.isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(validationRulesJson, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse validation rules: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Apply general validation rules
     */
    private void applyGeneralValidation(Map<String, Object> record, Map<String, Object> validationRules, List<String> errors) {
        // Required field validation
        Boolean requireAllFields = (Boolean) validationRules.get("requireAllFields");
        if (Boolean.TRUE.equals(requireAllFields)) {
            validateRequiredFields(record, errors);
        }

        // String length validation
        Integer maxStringLength = (Integer) validationRules.get("maxStringLength");
        if (maxStringLength != null) {
            validateStringLengths(record, maxStringLength, errors);
        }

        // Null/empty validation
        validateNotNullOrEmpty(record, errors);
    }

    /**
     * Apply guarantee-specific validation
     */
    private void applyGuaranteeValidation(Map<String, Object> record, List<String> errors) {
        // Guarantee reference validation
        String guaranteeRef = getString(record, "guaranteeReference");
        if (guaranteeRef != null && guaranteeRef.length() < 3) {
            errors.add("Guarantee reference must be at least 3 characters");
        }

        // Amount validation
        validateAmount(record, "amount", errors);

        // Currency validation
        validateCurrency(record, "currency", errors);

        // Date validation
        validateDate(record, "issueDate", "Issue date", errors);
        validateDate(record, "expiryDate", "Expiry date", errors);

        // Date logic validation
        validateDateLogic(record, errors);

        // Beneficiary validation
        validateNotEmpty(record, "beneficiaryName", "Beneficiary name", errors);

        // Applicant validation
        validateNotEmpty(record, "applicantName", "Applicant name", errors);

        // Email validation (if present)
        validateEmail(record, "beneficiaryEmail", false, errors);
        validateEmail(record, "applicantEmail", false, errors);
    }

    /**
     * Apply client-specific validation
     */
    private void applyClientValidation(Map<String, Object> record, List<String> errors) {
        // Client name validation
        validateNotEmpty(record, "name", "Client name", errors);

        // Email validation (required for clients)
        validateEmail(record, "email", true, errors);

        // Phone validation
        validatePhone(record, "phone", false, errors);

        // Address validation
        validateNotEmpty(record, "address", "Address", errors);

        // Tax ID validation (if present)
        String taxId = getString(record, "taxId");
        if (taxId != null && taxId.trim().length() < 5) {
            errors.add("Tax ID must be at least 5 characters");
        }
    }

    /**
     * Apply commission-specific validation
     */
    private void applyCommissionValidation(Map<String, Object> record, List<String> errors) {
        // Commission type validation
        validateNotEmpty(record, "commissionType", "Commission type", errors);

        // Rate validation
        validateCommissionRate(record, "rate", errors);

        // Amount validation
        validateAmount(record, "amount", errors);

        // Currency validation
        validateCurrency(record, "currency", errors);

        // Guarantee reference validation
        validateNotEmpty(record, "guaranteeReference", "Guarantee reference", errors);
    }

    // Specific validation methods
    private void validateRequiredFields(Map<String, Object> record, List<String> errors) {
        Set<String> requiredFields = Set.of("id", "name", "type");
        
        for (String field : requiredFields) {
            if (!record.containsKey(field) || record.get(field) == null || 
                record.get(field).toString().trim().isEmpty()) {
                errors.add("Required field missing or empty: " + field);
            }
        }
    }

    private void validateStringLengths(Map<String, Object> record, int maxLength, List<String> errors) {
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if (value.length() > maxLength) {
                    errors.add("Field " + entry.getKey() + " exceeds maximum length of " + maxLength);
                }
            }
        }
    }

    private void validateNotNullOrEmpty(Map<String, Object> record, List<String> errors) {
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            Object value = entry.getValue();
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                // Only add error for critical fields
                if (isCriticalField(entry.getKey())) {
                    errors.add("Critical field cannot be null or empty: " + entry.getKey());
                }
            }
        }
    }

    private void validateAmount(Map<String, Object> record, String fieldName, List<String> errors) {
        Object amountObj = record.get(fieldName);
        if (amountObj == null) return;

        try {
            BigDecimal amount = new BigDecimal(amountObj.toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(fieldName + " must be greater than zero");
            }
            if (amount.scale() > 2) {
                errors.add(fieldName + " cannot have more than 2 decimal places");
            }
        } catch (NumberFormatException e) {
            errors.add(fieldName + " must be a valid number");
        }
    }

    private void validateCurrency(Map<String, Object> record, String fieldName, List<String> errors) {
        String currency = getString(record, fieldName);
        if (currency == null) return;

        if (!CURRENCY_PATTERN.matcher(currency).matches()) {
            errors.add(fieldName + " must be a valid 3-letter currency code (e.g., USD, EUR)");
        }
    }

    private void validateDate(Map<String, Object> record, String fieldName, String displayName, List<String> errors) {
        String dateStr = getString(record, fieldName);
        if (dateStr == null) return;

        try {
            LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            errors.add(displayName + " must be in valid date format (YYYY-MM-DD)");
        }
    }

    private void validateDateLogic(Map<String, Object> record, List<String> errors) {
        String issueDateStr = getString(record, "issueDate");
        String expiryDateStr = getString(record, "expiryDate");

        if (issueDateStr == null || expiryDateStr == null) return;

        try {
            LocalDate issueDate = LocalDate.parse(issueDateStr);
            LocalDate expiryDate = LocalDate.parse(expiryDateStr);

            if (!expiryDate.isAfter(issueDate)) {
                errors.add("Expiry date must be after issue date");
            }

            // Check if expiry date is too far in the future (e.g., more than 10 years)
            if (expiryDate.isAfter(issueDate.plusYears(10))) {
                errors.add("Expiry date cannot be more than 10 years from issue date");
            }
        } catch (DateTimeParseException e) {
            // Already handled by individual date validation
        }
    }

    private void validateNotEmpty(Map<String, Object> record, String fieldName, String displayName, List<String> errors) {
        String value = getString(record, fieldName);
        if (value == null || value.trim().isEmpty()) {
            errors.add(displayName + " cannot be empty");
        }
    }

    private void validateEmail(Map<String, Object> record, String fieldName, boolean required, List<String> errors) {
        String email = getString(record, fieldName);
        
        if (email == null || email.trim().isEmpty()) {
            if (required) {
                errors.add("Email address is required");
            }
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Invalid email format: " + email);
        }
    }

    private void validatePhone(Map<String, Object> record, String fieldName, boolean required, List<String> errors) {
        String phone = getString(record, fieldName);
        
        if (phone == null || phone.trim().isEmpty()) {
            if (required) {
                errors.add("Phone number is required");
            }
            return;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            errors.add("Invalid phone number format: " + phone);
        }
    }

    private void validateCommissionRate(Map<String, Object> record, String fieldName, List<String> errors) {
        Object rateObj = record.get(fieldName);
        if (rateObj == null) return;

        try {
            BigDecimal rate = new BigDecimal(rateObj.toString());
            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Commission rate cannot be negative");
            }
            if (rate.compareTo(new BigDecimal("100")) > 0) {
                errors.add("Commission rate cannot exceed 100%");
            }
        } catch (NumberFormatException e) {
            errors.add("Commission rate must be a valid number");
        }
    }

    // Helper methods
    private String getString(Map<String, Object> record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? value.toString().trim() : null;
    }

    private boolean isCriticalField(String fieldName) {
        Set<String> criticalFields = Set.of("id", "name", "type", "amount", "currency", 
                                           "guaranteeReference", "beneficiaryName", "applicantName");
        return criticalFields.contains(fieldName);
    }
}




