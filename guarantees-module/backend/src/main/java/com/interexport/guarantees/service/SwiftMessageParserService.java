package com.interexport.guarantees.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interexport.guarantees.entity.enums.SwiftMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing SWIFT messages into structured data
 * Supports F7 - SWIFT Integration with MT760, MT765, MT767, MT768, MT769 parsing
 */
@Service
public class SwiftMessageParserService {

    private static final Logger logger = LoggerFactory.getLogger(SwiftMessageParserService.class);
    
    private final ObjectMapper objectMapper;
    
    // SWIFT field patterns (simplified - in production would use proper SWIFT parser library)
    private static final Pattern FIELD_PATTERN = Pattern.compile(":(\\d{2}[A-Z]?):(.*?)(?=:\\d{2}[A-Z]?:|$)", Pattern.DOTALL);
    private static final Pattern MESSAGE_TYPE_PATTERN = Pattern.compile("\\{1:F(\\d{2})(\\d{3}).*?\\}");
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\{4:[\\r\\n]*:(20):(.*?)[\\r\\n]", Pattern.DOTALL);

    public SwiftMessageParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse raw SWIFT message and extract structured fields
     */
    public SwiftMessageParseResult parseMessage(String rawMessage, SwiftMessageType expectedType) {
        try {
            logger.debug("Parsing SWIFT message of type: {}", expectedType);
            
            SwiftMessageParseResult result = new SwiftMessageParseResult();
            Map<String, Object> parsedFields = new HashMap<>();
            
            // Extract basic message info
            extractBasicInfo(rawMessage, parsedFields);
            
            // Extract SWIFT fields based on message type
            switch (expectedType) {
                case MT760:
                    parseMT760Fields(rawMessage, parsedFields);
                    break;
                case MT765:
                    parseMT765Fields(rawMessage, parsedFields);
                    break;
                case MT767:
                    parseMT767Fields(rawMessage, parsedFields);
                    break;
                case MT768:
                    parseMT768Fields(rawMessage, parsedFields);
                    break;
                case MT769:
                    parseMT769Fields(rawMessage, parsedFields);
                    break;
                case MT798:
                    parseMT798Fields(rawMessage, parsedFields);
                    break;
                default:
                    logger.warn("Unknown SWIFT message type: {}", expectedType);
            }
            
            // Convert to JSON
            result.setParsedFields(objectMapper.writeValueAsString(parsedFields));
            result.setMessageReference(extractString(parsedFields, "messageReference"));
            result.setSenderBic(extractString(parsedFields, "senderBic"));
            result.setReceiverBic(extractString(parsedFields, "receiverBic"));
            result.setTransactionReference(extractString(parsedFields, "transactionReference"));
            result.setSuccess(true);
            
            logger.debug("Successfully parsed SWIFT message with {} fields", parsedFields.size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing SWIFT message: {}", e.getMessage(), e);
            
            SwiftMessageParseResult errorResult = new SwiftMessageParseResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("Parse error: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Extract basic message information (header, sender, receiver, etc.)
     */
    private void extractBasicInfo(String rawMessage, Map<String, Object> fields) {
        // Extract message type
        Matcher messageTypeMatcher = MESSAGE_TYPE_PATTERN.matcher(rawMessage);
        if (messageTypeMatcher.find()) {
            fields.put("messageType", "MT" + messageTypeMatcher.group(1) + messageTypeMatcher.group(2));
        }
        
        // Extract message reference from field :20:
        Matcher refMatcher = REFERENCE_PATTERN.matcher(rawMessage);
        if (refMatcher.find()) {
            fields.put("messageReference", refMatcher.group(2).trim());
        }
        
        // Extract sender/receiver BIC (simplified - would use proper header parsing)
        extractBicsFromHeader(rawMessage, fields);
        
        // Extract common fields
        extractCommonFields(rawMessage, fields);
    }

    /**
     * Extract sender and receiver BIC codes from SWIFT header
     */
    private void extractBicsFromHeader(String rawMessage, Map<String, Object> fields) {
        // Simplified BIC extraction - in production would parse proper SWIFT headers
        Pattern bicPattern = Pattern.compile("([A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?)");
        Matcher bicMatcher = bicPattern.matcher(rawMessage);
        
        if (bicMatcher.find()) {
            fields.put("senderBic", bicMatcher.group(1));
        }
        if (bicMatcher.find()) {
            fields.put("receiverBic", bicMatcher.group(1));
        }
    }

    /**
     * Extract common SWIFT fields present in most message types
     */
    private void extractCommonFields(String rawMessage, Map<String, Object> fields) {
        Map<String, String> commonFields = new HashMap<>();
        
        // Field :20: - Transaction Reference
        commonFields.put("20", "transactionReference");
        
        // Field :21: - Related Reference
        commonFields.put("21", "relatedReference");
        
        // Field :25: - Account Identification
        commonFields.put("25", "accountIdentification");
        
        // Field :30: - Date
        commonFields.put("30", "issueDate");
        
        // Field :31D: - Expiry Date
        commonFields.put("31D", "expiryDate");
        
        // Field :32B: - Currency and Amount
        commonFields.put("32B", "currencyAmount");
        
        // Field :50: - Applicant/Ordering Customer
        commonFields.put("50", "applicant");
        
        // Field :59: - Beneficiary
        commonFields.put("59", "beneficiary");
        
        extractFieldsFromPattern(rawMessage, commonFields, fields);
    }

    /**
     * Parse MT760 (Received Guarantee) specific fields
     */
    private void parseMT760Fields(String rawMessage, Map<String, Object> fields) {
        fields.put("messageTypeDescription", "Received Guarantee");
        
        Map<String, String> mt760Fields = new HashMap<>();
        mt760Fields.put("40A", "formOfUndertaking");
        mt760Fields.put("77C", "guaranteeText");
        mt760Fields.put("40F", "applicableRules");
        mt760Fields.put("31D", "expiryDetails");
        mt760Fields.put("52A", "issuingBank");
        
        extractFieldsFromPattern(rawMessage, mt760Fields, fields);
    }

    /**
     * Parse MT765 (Guarantee Amendment) specific fields
     */
    private void parseMT765Fields(String rawMessage, Map<String, Object> fields) {
        fields.put("messageTypeDescription", "Guarantee Amendment");
        
        Map<String, String> mt765Fields = new HashMap<>();
        mt765Fields.put("23", "amendmentType");
        mt765Fields.put("77A", "amendmentDetails");
        mt765Fields.put("77C", "amendedText");
        
        extractFieldsFromPattern(rawMessage, mt765Fields, fields);
    }

    /**
     * Parse MT767 (Amendment Processing) specific fields
     */
    private void parseMT767Fields(String rawMessage, Map<String, Object> fields) {
        fields.put("messageTypeDescription", "Amendment Processing");
        
        Map<String, String> mt767Fields = new HashMap<>();
        mt767Fields.put("23", "processingCode");
        mt767Fields.put("77A", "processingDetails");
        
        extractFieldsFromPattern(rawMessage, mt767Fields, fields);
    }

    /**
     * Parse MT768 (Acknowledgment) specific fields
     */
    private void parseMT768Fields(String rawMessage, Map<String, Object> fields) {
        fields.put("messageTypeDescription", "Acknowledgment");
        
        Map<String, String> mt768Fields = new HashMap<>();
        mt768Fields.put("77A", "acknowledgmentText");
        
        extractFieldsFromPattern(rawMessage, mt768Fields, fields);
    }

    /**
     * Parse MT769 (Discrepancy Advice) specific fields
     */
    private void parseMT769Fields(String rawMessage, Map<String, Object> fields) {
        fields.put("messageTypeDescription", "Discrepancy Advice");
        
        Map<String, String> mt769Fields = new HashMap<>();
        mt769Fields.put("77A", "discrepancyDetails");
        
        extractFieldsFromPattern(rawMessage, mt769Fields, fields);
    }

    /**
     * Parse MT798 (Free Format Message) specific fields
     */
    private void parseMT798Fields(String rawMessage, Map<String, Object> fields) {
        fields.put("messageTypeDescription", "Free Format Message");
        
        Map<String, String> mt798Fields = new HashMap<>();
        mt798Fields.put("77A", "freeText");
        
        extractFieldsFromPattern(rawMessage, mt798Fields, fields);
    }

    /**
     * Extract specific fields from raw message using field patterns
     */
    private void extractFieldsFromPattern(String rawMessage, Map<String, String> fieldMappings, 
                                        Map<String, Object> results) {
        Matcher fieldMatcher = FIELD_PATTERN.matcher(rawMessage);
        
        while (fieldMatcher.find()) {
            String fieldTag = fieldMatcher.group(1);
            String fieldValue = fieldMatcher.group(2).trim();
            
            if (fieldMappings.containsKey(fieldTag)) {
                String mappedName = fieldMappings.get(fieldTag);
                results.put(mappedName, fieldValue);
                results.put("field_" + fieldTag, fieldValue); // Also keep raw field reference
            }
        }
    }

    /**
     * Safely extract string value from parsed fields
     */
    private String extractString(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value != null ? value.toString().trim() : null;
    }

    /**
     * Validate parsed message fields for business rules
     */
    public SwiftMessageValidationResult validateParsedMessage(String parsedFields, SwiftMessageType messageType) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = objectMapper.readValue(parsedFields, Map.class);
            
            SwiftMessageValidationResult result = new SwiftMessageValidationResult();
            result.setValid(true);
            
            // Basic validation
            if (fields.get("transactionReference") == null || 
                fields.get("transactionReference").toString().trim().isEmpty()) {
                result.addError("Missing transaction reference (field :20:)");
            }
            
            // Message type specific validation
            switch (messageType) {
                case MT760:
                    validateMT760Fields(fields, result);
                    break;
                case MT765:
                    validateMT765Fields(fields, result);
                    break;
                // Add other validations as needed
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating parsed message: {}", e.getMessage(), e);
            
            SwiftMessageValidationResult errorResult = new SwiftMessageValidationResult();
            errorResult.setValid(false);
            errorResult.addError("Validation error: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Validate MT760 specific business rules
     */
    private void validateMT760Fields(Map<String, Object> fields, SwiftMessageValidationResult result) {
        // Validate guarantee amount
        if (fields.get("currencyAmount") == null) {
            result.addError("Missing guarantee amount (field :32B:)");
        }
        
        // Validate expiry date
        if (fields.get("expiryDate") == null && fields.get("expiryDetails") == null) {
            result.addError("Missing expiry information (field :31D:)");
        }
        
        // Validate beneficiary
        if (fields.get("beneficiary") == null) {
            result.addError("Missing beneficiary information (field :59:)");
        }
    }

    /**
     * Validate MT765 specific business rules
     */
    private void validateMT765Fields(Map<String, Object> fields, SwiftMessageValidationResult result) {
        // Validate amendment details
        if (fields.get("amendmentDetails") == null) {
            result.addError("Missing amendment details (field :77A:)");
        }
        
        // Validate related reference
        if (fields.get("relatedReference") == null) {
            result.addError("Missing related guarantee reference (field :21:)");
        }
    }

    /**
     * Result class for SWIFT message parsing
     */
    public static class SwiftMessageParseResult {
        private boolean success;
        private String errorMessage;
        private String parsedFields;
        private String messageReference;
        private String senderBic;
        private String receiverBic;
        private String transactionReference;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getParsedFields() { return parsedFields; }
        public void setParsedFields(String parsedFields) { this.parsedFields = parsedFields; }
        
        public String getMessageReference() { return messageReference; }
        public void setMessageReference(String messageReference) { this.messageReference = messageReference; }
        
        public String getSenderBic() { return senderBic; }
        public void setSenderBic(String senderBic) { this.senderBic = senderBic; }
        
        public String getReceiverBic() { return receiverBic; }
        public void setReceiverBic(String receiverBic) { this.receiverBic = receiverBic; }
        
        public String getTransactionReference() { return transactionReference; }
        public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    }

    /**
     * Result class for SWIFT message validation
     */
    public static class SwiftMessageValidationResult {
        private boolean valid;
        private java.util.List<String> errors = new java.util.ArrayList<>();
        
        public boolean isValid() { return valid && errors.isEmpty(); }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public java.util.List<String> getErrors() { return errors; }
        public void setErrors(java.util.List<String> errors) { this.errors = errors; }
        
        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }
        
        public String getErrorsAsString() {
            return String.join("; ", errors);
        }
    }
}




