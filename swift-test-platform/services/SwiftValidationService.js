class SwiftValidationService {
  constructor() {
    this.validationRules = this.initializeValidationRules();
    this.fieldValidators = this.initializeFieldValidators();
  }

  /**
   * Validate SWIFT message
   */
  validateMessage(messageType, content) {
    const errors = [];
    const warnings = [];

    try {
      // Check if message type is supported
      if (!this.validationRules[messageType]) {
        errors.push(`Unsupported message type: ${messageType}`);
        return { isValid: false, errors, warnings };
      }

      const rules = this.validationRules[messageType];

      // Validate required fields
      this.validateRequiredFields(rules.requiredFields, content, errors);

      // Validate field formats
      this.validateFieldFormats(messageType, content, errors, warnings);

      // Validate business rules
      this.validateBusinessRules(messageType, content, errors, warnings);

      // Validate SWIFT format compliance
      this.validateSwiftCompliance(messageType, content, errors, warnings);

      return {
        isValid: errors.length === 0,
        errors,
        warnings,
        messageType,
        validatedFields: Object.keys(content).length
      };

    } catch (error) {
      errors.push(`Validation error: ${error.message}`);
      return { isValid: false, errors, warnings };
    }
  }

  /**
   * Validate required fields
   */
  validateRequiredFields(requiredFields, content, errors) {
    requiredFields.forEach(field => {
      if (!content[field] || content[field].toString().trim() === '') {
        errors.push(`Missing required field: ${field}`);
      }
    });
  }

  /**
   * Validate field formats
   */
  validateFieldFormats(messageType, content, errors, warnings) {
    Object.keys(content).forEach(field => {
      const value = content[field];
      const validator = this.fieldValidators[field];

      if (validator && !validator.validate(value)) {
        if (validator.severity === 'error') {
          errors.push(`Invalid format for field ${field}: ${validator.message}`);
        } else {
          warnings.push(`Format warning for field ${field}: ${validator.message}`);
        }
      }
    });
  }

  /**
   * Validate business rules
   */
  validateBusinessRules(messageType, content, errors, warnings) {
    switch (messageType) {
      case 'MT760':
        this.validateMT760BusinessRules(content, errors, warnings);
        break;
      case 'MT765':
        this.validateMT765BusinessRules(content, errors, warnings);
        break;
      case 'MT767':
        this.validateMT767BusinessRules(content, errors, warnings);
        break;
      case 'MT769':
        this.validateMT769BusinessRules(content, errors, warnings);
        break;
    }
  }

  /**
   * Validate MT760 business rules
   */
  validateMT760BusinessRules(content, errors, warnings) {
    // Check guarantee amount
    if (content.guaranteeAmount) {
      const amount = parseFloat(content.guaranteeAmount);
      if (amount <= 0) {
        errors.push('Guarantee amount must be greater than zero');
      }
      if (amount > 10000000) {
        warnings.push('Large guarantee amount detected, please verify');
      }
    }

    // Check dates
    if (content.issueDate && content.expiryDate) {
      const issueDate = new Date(content.issueDate);
      const expiryDate = new Date(content.expiryDate);
      
      if (expiryDate <= issueDate) {
        errors.push('Expiry date must be after issue date');
      }

      const diffYears = (expiryDate - issueDate) / (365 * 24 * 60 * 60 * 1000);
      if (diffYears > 10) {
        warnings.push('Guarantee validity period exceeds 10 years');
      }
    }

    // Check currency
    if (content.currency && !this.isValidCurrency(content.currency)) {
      errors.push(`Invalid currency code: ${content.currency}`);
    }

    // Check BIC codes
    if (content.applicantBIC && !this.isValidBIC(content.applicantBIC)) {
      errors.push(`Invalid applicant BIC: ${content.applicantBIC}`);
    }
    if (content.beneficiaryBIC && !this.isValidBIC(content.beneficiaryBIC)) {
      errors.push(`Invalid beneficiary BIC: ${content.beneficiaryBIC}`);
    }
  }

  /**
   * Validate MT765 business rules
   */
  validateMT765BusinessRules(content, errors, warnings) {
    // Check amendment reference exists
    if (!content.amendmentReference) {
      errors.push('Amendment reference is required for MT765');
    }

    // Check original guarantee reference
    if (!content.originalReference) {
      errors.push('Original guarantee reference is required for amendments');
    }

    // Validate amendment type
    if (content.amendmentType) {
      const validTypes = ['AMOUNT_INCREASE', 'AMOUNT_DECREASE', 'EXPIRY_EXTENSION', 'EXPIRY_REDUCTION', 'TEXT_AMENDMENT'];
      if (!validTypes.includes(content.amendmentType)) {
        warnings.push(`Amendment type ${content.amendmentType} is not standard`);
      }
    }

    // Check new values based on amendment type
    if (content.amendmentType === 'AMOUNT_INCREASE' || content.amendmentType === 'AMOUNT_DECREASE') {
      if (!content.newValue || parseFloat(content.newValue) <= 0) {
        errors.push('New amount value is required for amount amendments');
      }
    }
  }

  /**
   * Validate MT767 business rules
   */
  validateMT767BusinessRules(content, errors, warnings) {
    // Check confirmation type
    if (content.confirmationType) {
      const validTypes = ['ACCEPTED', 'REJECTED', 'PARTIAL'];
      if (!validTypes.includes(content.confirmationType)) {
        warnings.push(`Confirmation type ${content.confirmationType} is not standard`);
      }
    }

    // Check amendment reference
    if (!content.amendmentReference && !content.originalReference) {
      errors.push('Either amendment reference or original reference is required');
    }
  }

  /**
   * Validate MT769 business rules
   */
  validateMT769BusinessRules(content, errors, warnings) {
    // Check claim amount
    if (content.claimAmount && parseFloat(content.claimAmount) <= 0) {
      errors.push('Claim amount must be greater than zero');
    }

    // Check claim reason
    if (!content.claimReason || content.claimReason.trim().length < 10) {
      errors.push('Detailed claim reason is required (minimum 10 characters)');
    }

    // Check original reference
    if (!content.originalReference) {
      errors.push('Original guarantee reference is required for claims');
    }
  }

  /**
   * Validate SWIFT format compliance
   */
  validateSwiftCompliance(messageType, content, errors, warnings) {
    // Check character encoding (SWIFT allows specific character set)
    Object.keys(content).forEach(field => {
      const value = content[field].toString();
      if (!/^[A-Z0-9\s\.,\-()/?':+]*$/i.test(value)) {
        warnings.push(`Field ${field} contains non-SWIFT compliant characters`);
      }
    });

    // Check field length limits
    this.validateFieldLengths(messageType, content, errors, warnings);

    // Check mandatory field sequences for specific message types
    this.validateFieldSequence(messageType, content, errors);
  }

  /**
   * Validate field lengths according to SWIFT standards
   */
  validateFieldLengths(messageType, content, errors, warnings) {
    const lengthLimits = {
      'transactionReference': 16,
      'originalReference': 16,
      'amendmentReference': 16,
      'claimReference': 16,
      'guaranteeText': 780, // Multiple lines allowed
      'amendmentReason': 780,
      'claimReason': 780,
      'applicantName': 140,
      'beneficiaryName': 140,
      'currency': 3
    };

    Object.keys(lengthLimits).forEach(field => {
      if (content[field] && content[field].toString().length > lengthLimits[field]) {
        errors.push(`Field ${field} exceeds maximum length of ${lengthLimits[field]} characters`);
      }
    });
  }

  /**
   * Validate field sequence (some SWIFT messages require specific field ordering)
   */
  validateFieldSequence(messageType, content, errors) {
    // Field sequence validation for specific message types
    const sequenceRules = {
      'MT760': ['transactionReference', 'issueDate', 'expiryDate', 'guaranteeAmount', 'applicantName', 'beneficiaryName'],
      'MT765': ['amendmentReference', 'originalReference', 'amendmentType', 'newValue'],
      'MT767': ['amendmentReference', 'confirmationType', 'confirmationText'],
      'MT769': ['claimReference', 'originalReference', 'claimAmount', 'claimReason']
    };

    const expectedSequence = sequenceRules[messageType];
    if (expectedSequence) {
      const contentKeys = Object.keys(content);
      let currentIndex = 0;

      expectedSequence.forEach(expectedField => {
        if (content[expectedField]) {
          const actualIndex = contentKeys.indexOf(expectedField);
          if (actualIndex < currentIndex) {
            warnings.push(`Field ${expectedField} appears out of expected sequence`);
          }
          currentIndex = Math.max(currentIndex, actualIndex + 1);
        }
      });
    }
  }

  /**
   * Initialize validation rules for each message type
   */
  initializeValidationRules() {
    return {
      'MT760': {
        name: 'Issue of a Guarantee',
        requiredFields: ['transactionReference', 'guaranteeAmount', 'currency', 'applicantName', 'beneficiaryName'],
        optionalFields: ['issueDate', 'expiryDate', 'guaranteeText', 'underlyingContract']
      },
      'MT765': {
        name: 'Amendment to a Guarantee',
        requiredFields: ['amendmentReference', 'originalReference', 'amendmentType'],
        optionalFields: ['newValue', 'amendmentReason', 'effectiveDate']
      },
      'MT767': {
        name: 'Confirmation of Amendment',
        requiredFields: ['amendmentReference', 'confirmationType'],
        optionalFields: ['confirmationText', 'processedDate']
      },
      'MT768': {
        name: 'Acknowledgment',
        requiredFields: ['originalReference', 'acknowledgmentType'],
        optionalFields: ['acknowledgmentText']
      },
      'MT769': {
        name: 'Advice of Discrepancy',
        requiredFields: ['claimReference', 'originalReference', 'claimReason'],
        optionalFields: ['claimAmount', 'documentsRequired', 'processingDeadline']
      },
      'MT798': {
        name: 'Free Format Message',
        requiredFields: ['transactionReference'],
        optionalFields: ['messageText', 'relatedReference']
      }
    };
  }

  /**
   * Initialize field validators
   */
  initializeFieldValidators() {
    return {
      'transactionReference': {
        validate: (value) => /^[A-Z0-9]{1,16}$/.test(value),
        message: 'Must be alphanumeric, max 16 characters',
        severity: 'error'
      },
      'guaranteeAmount': {
        validate: (value) => /^\d+(\.\d{1,2})?$/.test(value) && parseFloat(value) > 0,
        message: 'Must be a positive number with max 2 decimal places',
        severity: 'error'
      },
      'currency': {
        validate: (value) => this.isValidCurrency(value),
        message: 'Must be a valid 3-letter ISO currency code',
        severity: 'error'
      },
      'issueDate': {
        validate: (value) => this.isValidDate(value),
        message: 'Must be a valid date in YYYY-MM-DD format',
        severity: 'error'
      },
      'expiryDate': {
        validate: (value) => this.isValidDate(value),
        message: 'Must be a valid date in YYYY-MM-DD format',
        severity: 'error'
      },
      'applicantBIC': {
        validate: (value) => this.isValidBIC(value),
        message: 'Must be a valid BIC code (8 or 11 characters)',
        severity: 'warning'
      },
      'beneficiaryBIC': {
        validate: (value) => this.isValidBIC(value),
        message: 'Must be a valid BIC code (8 or 11 characters)',
        severity: 'warning'
      }
    };
  }

  /**
   * Check if currency code is valid
   */
  isValidCurrency(currency) {
    const validCurrencies = [
      'USD', 'EUR', 'GBP', 'JPY', 'CHF', 'CAD', 'AUD', 'CNY', 'INR', 'BRL',
      'KRW', 'SGD', 'HKD', 'NOK', 'SEK', 'DKK', 'PLN', 'CZK', 'HUF', 'RUB',
      'TRY', 'ZAR', 'MXN', 'ARS', 'CLP', 'PEN', 'COP', 'THB', 'MYR', 'IDR',
      'PHP', 'VND', 'EGP', 'MAD', 'NGN', 'KES', 'GHS', 'UGX', 'TZS'
    ];
    return validCurrencies.includes(currency);
  }

  /**
   * Check if BIC code is valid
   */
  isValidBIC(bic) {
    // BIC format: 4 letters (bank code) + 2 letters (country) + 2 characters (location) + optional 3 characters (branch)
    return /^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$/.test(bic);
  }

  /**
   * Check if date is valid
   */
  isValidDate(date) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      return false;
    }
    const parsed = new Date(date);
    return parsed instanceof Date && !isNaN(parsed);
  }

  /**
   * Get validation rules for a specific message type
   */
  getValidationRules(messageType) {
    return this.validationRules[messageType] || null;
  }

  /**
   * Get all supported message types
   */
  getSupportedMessageTypes() {
    return Object.keys(this.validationRules);
  }

  /**
   * Validate raw SWIFT message format
   */
  validateRawSwiftFormat(rawMessage) {
    const errors = [];
    const warnings = [];

    // Check basic SWIFT message structure
    if (!rawMessage.startsWith('{1:')) {
      errors.push('Invalid SWIFT message: Missing block 1 (Basic Header)');
    }

    if (!rawMessage.includes('{2:')) {
      errors.push('Invalid SWIFT message: Missing block 2 (Application Header)');
    }

    if (!rawMessage.includes('{4:')) {
      errors.push('Invalid SWIFT message: Missing block 4 (Text Block)');
    }

    // Check for properly closed blocks
    const openBraces = (rawMessage.match(/{/g) || []).length;
    const closeBraces = (rawMessage.match(/}/g) || []).length;
    if (openBraces !== closeBraces) {
      errors.push('Invalid SWIFT message: Unbalanced braces');
    }

    // Check message type extraction
    const messageType = this.extractMessageType(rawMessage);
    if (messageType === 'UNKNOWN') {
      errors.push('Invalid SWIFT message: Cannot determine message type');
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
      messageType
    };
  }

  /**
   * Extract message type from raw SWIFT message
   */
  extractMessageType(rawMessage) {
    const match = rawMessage.match(/{2:I(\d{3})/);
    return match ? `MT${match[1]}` : 'UNKNOWN';
  }
}

module.exports = SwiftValidationService;




