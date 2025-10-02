package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.GuaranteeTemplate;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.repository.GuaranteeTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for guarantee template management and text generation
 * Implements F2 - Guarantee Texts (templates + variables)
 */
@Service
@Transactional
public class GuaranteeTemplateService {

    private final GuaranteeTemplateRepository templateRepository;
    
    // Pattern for variable placeholders: {{VARIABLE_NAME}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([A-Z_][A-Z0-9_]*)\\}\\}");
    
    // Date formatters for different contexts
    private static final DateTimeFormatter LONG_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMAL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    @Autowired
    public GuaranteeTemplateService(GuaranteeTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Select template by 22A/22D/language
     * UC2.1: Select template by 22A/22D/language
     */
    public GuaranteeTemplate selectTemplate(GuaranteeType guaranteeType, String language, String field22A, String field22D) {
        // First try to find specific template for the combination
        List<GuaranteeTemplate> templates = templateRepository
            .findBestMatchingTemplate(guaranteeType, language.toUpperCase());
        Optional<GuaranteeTemplate> specificTemplate = templates.isEmpty() ? Optional.empty() : Optional.of(templates.get(0));
            
        if (specificTemplate.isPresent()) {
            return specificTemplate.get();
        }
        
        // Fallback to default template for the guarantee type
        Optional<GuaranteeTemplate> defaultTemplate = templateRepository
            .findDefaultTemplate(guaranteeType, language);
            
        if (defaultTemplate.isPresent()) {
            return defaultTemplate.get();
        }
        
        throw new IllegalArgumentException(String.format(
            "No template found for guarantee type %s and language %s. " +
            "Please ensure appropriate templates are configured.",
            guaranteeType, language));
    }

    /**
     * Render preview with amount and date in words
     * UC2.2: Render preview with amount and date in words
     */
    public GuaranteeTextPreview renderPreview(GuaranteeTemplate template, GuaranteeContract guarantee) {
        Map<String, Object> variables = buildVariableMap(guarantee);
        
        // Generate preview text
        String previewText = renderTemplate(template.getTemplateText(), variables);
        
        // Identify missing required variables
        List<String> missingRequired = validateRequiredVariables(template, variables);
        
        // Get all variable placeholders found in template
        List<String> allVariables = template.extractVariablePlaceholders();
        
        return new GuaranteeTextPreview(
            previewText,
            variables,
            allVariables,
            missingRequired,
            template.getRequiredVariablesList(),
            template.getOptionalVariablesList()
        );
    }

    /**
     * Validate mandatory variables are present
     * UC2.4: Validate mandatory variables are present
     */
    public List<String> validateRequiredVariables(GuaranteeTemplate template, Map<String, Object> variables) {
        List<String> required = template.getRequiredVariablesList();
        List<String> missing = new ArrayList<>();
        
        for (String requiredVar : required) {
            if (!variables.containsKey(requiredVar) || variables.get(requiredVar) == null) {
                missing.add(requiredVar);
            }
        }
        
        return missing;
    }

    /**
     * Save final text version into the guarantee
     * UC2.5: Save final text version into the guarantee
     */
    @Transactional
    public GuaranteeContract saveRenderedText(GuaranteeContract guarantee, GuaranteeTemplate template, 
                                            Map<String, Object> customVariables) {
        
        // Merge default and custom variables
        Map<String, Object> allVariables = buildVariableMap(guarantee);
        if (customVariables != null) {
            allVariables.putAll(customVariables);
        }
        
        // Validate required variables
        List<String> missingRequired = validateRequiredVariables(template, allVariables);
        if (!missingRequired.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Cannot save guarantee text - missing required variables: %s. " +
                "Please provide values for all mandatory variables before saving.",
                String.join(", ", missingRequired)));
        }
        
        // Render final text
        String finalText = renderTemplate(template.getTemplateText(), allVariables);
        
        // Sanitize for XSS protection (UC2.5 requirement)
        String sanitizedText = sanitizeHtml(finalText);
        
        // Save to guarantee
        guarantee.setGuaranteeText(sanitizedText);
        guarantee.setLanguage(template.getLanguage());
        
        return guarantee;
    }

    /**
     * Core template rendering engine
     * UC2.2: Template engine replaces variables correctly
     */
    private String renderTemplate(String templateText, Map<String, Object> variables) {
        if (templateText == null) return "";
        
        StringBuilder result = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(templateText);
        int lastEnd = 0;
        
        while (matcher.find()) {
            // Add text before the variable
            result.append(templateText, lastEnd, matcher.start());
            
            String variableName = matcher.group(1);
            Object variableValue = variables.get(variableName);
            
            if (variableValue != null) {
                result.append(formatVariable(variableName, variableValue));
            } else {
                // Keep placeholder if variable not found
                result.append("{{").append(variableName).append("}}");
            }
            
            lastEnd = matcher.end();
        }
        
        // Add remaining text
        result.append(templateText.substring(lastEnd));
        
        return result.toString();
    }

    /**
     * Build comprehensive variable map from guarantee data
     */
    private Map<String, Object> buildVariableMap(GuaranteeContract guarantee) {
        Map<String, Object> variables = new HashMap<>();
        
        if (guarantee == null) return variables;
        
        // Basic guarantee information
        variables.put("GUARANTEE_REFERENCE", guarantee.getReference());
        variables.put("GUARANTEE_TYPE", guarantee.getGuaranteeType().name());
        variables.put("GUARANTEE_TYPE_DISPLAY", formatGuaranteeType(guarantee.getGuaranteeType()));
        
        // Amounts and currency
        variables.put("GUARANTEE_AMOUNT", guarantee.getAmount());
        variables.put("GUARANTEE_AMOUNT_FORMATTED", formatCurrency(guarantee.getAmount(), guarantee.getCurrency()));
        variables.put("GUARANTEE_AMOUNT_WORDS", convertAmountToWords(guarantee.getAmount(), guarantee.getCurrency()));
        variables.put("CURRENCY", guarantee.getCurrency());
        variables.put("CURRENCY_SYMBOL", getCurrencySymbol(guarantee.getCurrency()));
        
        // Dates
        if (guarantee.getIssueDate() != null) {
            variables.put("ISSUE_DATE", guarantee.getIssueDate().format(SHORT_DATE_FORMAT));
            variables.put("ISSUE_DATE_LONG", guarantee.getIssueDate().format(LONG_DATE_FORMAT));
            variables.put("ISSUE_DATE_FORMAL", guarantee.getIssueDate().format(FORMAL_DATE_FORMAT));
            variables.put("ISSUE_DATE_WORDS", convertDateToWords(guarantee.getIssueDate()));
        }
        
        if (guarantee.getExpiryDate() != null) {
            variables.put("EXPIRY_DATE", guarantee.getExpiryDate().format(SHORT_DATE_FORMAT));
            variables.put("EXPIRY_DATE_LONG", guarantee.getExpiryDate().format(LONG_DATE_FORMAT));
            variables.put("EXPIRY_DATE_FORMAL", guarantee.getExpiryDate().format(FORMAL_DATE_FORMAT));
            variables.put("EXPIRY_DATE_WORDS", convertDateToWords(guarantee.getExpiryDate()));
        }
        
        // Parties
        if (guarantee.getApplicantId() != null) {
            variables.put("APPLICANT_ID", guarantee.getApplicantId().toString());
        }
        variables.put("BENEFICIARY_NAME", guarantee.getBeneficiaryName());
        variables.put("BENEFICIARY_ADDRESS", guarantee.getBeneficiaryAddress());
        
        // Banking information
        variables.put("ADVISING_BANK_BIC", guarantee.getAdvisingBankBic());
        variables.put("IS_DOMESTIC", guarantee.getIsDomestic() ? "YES" : "NO");
        variables.put("DOMESTIC_INTERNATIONAL", guarantee.getIsDomestic() ? "Domestic" : "International");
        
        // Contract details
        variables.put("UNDERLYING_CONTRACT_REF", guarantee.getUnderlyingContractRef());
        variables.put("SPECIAL_CONDITIONS", guarantee.getSpecialConditions());
        
        // System information
        variables.put("CURRENT_DATE", LocalDate.now().format(SHORT_DATE_FORMAT));
        variables.put("CURRENT_DATE_LONG", LocalDate.now().format(LONG_DATE_FORMAT));
        variables.put("CURRENT_DATE_FORMAL", LocalDate.now().format(FORMAL_DATE_FORMAT));
        variables.put("CURRENT_YEAR", String.valueOf(LocalDate.now().getYear()));
        
        return variables;
    }

    /**
     * Format variable values based on type and context
     */
    private String formatVariable(String variableName, Object value) {
        if (value == null) return "";
        
        // Special formatting for specific variables
        if (variableName.contains("AMOUNT") && value instanceof BigDecimal) {
            return String.format("%,.2f", (BigDecimal) value);
        }
        
        return value.toString();
    }

    /**
     * Format currency amounts with proper symbols and formatting
     */
    private String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) return "";
        return String.format("%s %,.2f", getCurrencySymbol(currency), amount);
    }

    /**
     * Get currency symbol for display
     */
    private String getCurrencySymbol(String currency) {
        if (currency == null) return "";
        switch (currency.toUpperCase()) {
            case "USD": return "$";
            case "EUR": return "€";
            case "GBP": return "£";
            case "JPY": return "¥";
            default: return currency;
        }
    }

    /**
     * Convert amount to words (basic implementation)
     */
    private String convertAmountToWords(BigDecimal amount, String currency) {
        if (amount == null) return "";
        
        // This is a simplified implementation
        // In production, you'd use a proper number-to-words library
        long wholePart = amount.longValue();
        int decimalPart = amount.remainder(BigDecimal.ONE).multiply(new BigDecimal("100")).intValue();
        
        String wholeWords = convertNumberToWords(wholePart);
        String currencyName = getCurrencyName(currency);
        
        if (decimalPart > 0) {
            String decimalWords = convertNumberToWords(decimalPart);
            String centName = getCentName(currency);
            return String.format("%s %s and %s %s", wholeWords, currencyName, decimalWords, centName);
        } else {
            return String.format("%s %s", wholeWords, currencyName);
        }
    }

    /**
     * Convert date to words
     */
    private String convertDateToWords(LocalDate date) {
        if (date == null) return "";
        
        String day = convertNumberToWords(date.getDayOfMonth());
        String month = date.getMonth().toString().toLowerCase();
        month = month.substring(0, 1).toUpperCase() + month.substring(1);
        String year = convertNumberToWords(date.getYear());
        
        return String.format("%s day of %s, %s", day, month, year);
    }

    /**
     * Basic number to words conversion (simplified)
     */
    private String convertNumberToWords(long number) {
        if (number == 0) return "zero";
        if (number < 0) return "negative " + convertNumberToWords(-number);
        
        String[] ones = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
                        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", 
                        "seventeen", "eighteen", "nineteen"};
        
        String[] tens = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
        
        if (number < 20) {
            return ones[(int) number];
        } else if (number < 100) {
            return tens[(int) number / 10] + (number % 10 != 0 ? " " + ones[(int) number % 10] : "");
        } else if (number < 1000) {
            return ones[(int) number / 100] + " hundred" + (number % 100 != 0 ? " " + convertNumberToWords(number % 100) : "");
        } else if (number < 1000000) {
            return convertNumberToWords(number / 1000) + " thousand" + (number % 1000 != 0 ? " " + convertNumberToWords(number % 1000) : "");
        } else {
            return convertNumberToWords(number / 1000000) + " million" + (number % 1000000 != 0 ? " " + convertNumberToWords(number % 1000000) : "");
        }
    }

    /**
     * Get currency name for words conversion
     */
    private String getCurrencyName(String currency) {
        if (currency == null) return "units";
        switch (currency.toUpperCase()) {
            case "USD": return "dollars";
            case "EUR": return "euros";
            case "GBP": return "pounds";
            case "JPY": return "yen";
            default: return currency.toLowerCase();
        }
    }

    /**
     * Get cent name for words conversion
     */
    private String getCentName(String currency) {
        if (currency == null) return "cents";
        switch (currency.toUpperCase()) {
            case "USD": return "cents";
            case "EUR": return "cents";
            case "GBP": return "pence";
            case "JPY": return "sen";
            default: return "cents";
        }
    }

    /**
     * Format guarantee type for display
     */
    private String formatGuaranteeType(GuaranteeType type) {
        if (type == null) return "";
        switch (type) {
            case PERFORMANCE: return "Performance Guarantee";
            case ADVANCE_PAYMENT: return "Advance Payment Guarantee";
            case BID_BOND: return "Bid Bond";
            case WARRANTY: return "Warranty Bond";
            case CUSTOMS: return "Customs Guarantee";
            case PAYMENT: return "Payment Guarantee";
            default: return type.name();
        }
    }

    /**
     * Sanitize HTML to prevent XSS attacks
     * UC2.5: HTML sanitization to avoid XSS
     */
    private String sanitizeHtml(String input) {
        if (input == null) return "";
        
        // Basic HTML sanitization - remove potential script tags and dangerous attributes
        // In production, use a proper HTML sanitization library like OWASP Java HTML Sanitizer
        return input
            .replaceAll("<script[^>]*>.*?</script>", "")
            .replaceAll("<iframe[^>]*>.*?</iframe>", "")
            .replaceAll("javascript:", "")
            .replaceAll("on\\w+\\s*=", "")
            .trim();
    }

    /**
     * Get all available templates
     */
    @Transactional(readOnly = true)
    public List<GuaranteeTemplate> getAllActiveTemplates() {
        return templateRepository.findAllActiveTemplatesOrderByTypeAndLanguage();
    }

    /**
     * Get templates by guarantee type
     */
    @Transactional(readOnly = true)
    public List<GuaranteeTemplate> getTemplatesByType(GuaranteeType guaranteeType) {
        return templateRepository.findActiveTemplatesByType(guaranteeType);
    }

    /**
     * Create new template
     */
    @Transactional
    public GuaranteeTemplate createTemplate(GuaranteeTemplate template) {
        // Validate template text contains valid variables
        List<String> extractedVars = template.extractVariablePlaceholders();
        template.setRequiredVariablesList(extractedVars);
        
        return templateRepository.save(template);
    }

    /**
     * Update existing template
     */
    @Transactional
    public GuaranteeTemplate updateTemplate(Long id, GuaranteeTemplate updatedTemplate) {
        GuaranteeTemplate existing = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));
            
        // Update fields
        existing.setTemplateName(updatedTemplate.getTemplateName());
        existing.setTemplateText(updatedTemplate.getTemplateText());
        existing.setDescription(updatedTemplate.getDescription());
        existing.setIsActive(updatedTemplate.getIsActive());
        existing.setIsDefault(updatedTemplate.getIsDefault());
        existing.setUsageNotes(updatedTemplate.getUsageNotes());
        
        // Re-extract variables from updated text
        List<String> extractedVars = existing.extractVariablePlaceholders();
        existing.setRequiredVariablesList(extractedVars);
        
        return templateRepository.save(existing);
    }

    /**
     * Result class for template preview
     */
    public static class GuaranteeTextPreview {
        private final String previewText;
        private final Map<String, Object> variables;
        private final List<String> allVariables;
        private final List<String> missingRequired;
        private final List<String> requiredVariables;
        private final List<String> optionalVariables;

        public GuaranteeTextPreview(String previewText, Map<String, Object> variables,
                                  List<String> allVariables, List<String> missingRequired,
                                  List<String> requiredVariables, List<String> optionalVariables) {
            this.previewText = previewText;
            this.variables = variables;
            this.allVariables = allVariables;
            this.missingRequired = missingRequired;
            this.requiredVariables = requiredVariables;
            this.optionalVariables = optionalVariables;
        }

        // Getters
        public String getPreviewText() { return previewText; }
        public Map<String, Object> getVariables() { return variables; }
        public List<String> getAllVariables() { return allVariables; }
        public List<String> getMissingRequired() { return missingRequired; }
        public List<String> getRequiredVariables() { return requiredVariables; }
        public List<String> getOptionalVariables() { return optionalVariables; }
        public boolean isValid() { return missingRequired.isEmpty(); }
    }
}
