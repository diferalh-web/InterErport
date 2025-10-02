const SwiftValidationService = require('../../services/SwiftValidationService');

describe('SwiftValidationService', () => {
  let validationService;

  beforeEach(() => {
    validationService = new SwiftValidationService();
  });

  describe('validateBIC', () => {
    it('should validate correct BIC format', () => {
      const validBICs = [
        'TESTBANKXXX',
        'INTEXPORXXXX',
        'DEUTDEFFXXX',
        'CHASUS33XXX'
      ];

      validBICs.forEach(bic => {
        expect(validationService.validateBIC(bic)).toBe(true);
      });
    });

    it('should reject invalid BIC format', () => {
      const invalidBICs = [
        'INVALID',
        'TESTBANK',
        'TESTBANKXXXXX',
        '123456789',
        ''
      ];

      invalidBICs.forEach(bic => {
        expect(validationService.validateBIC(bic)).toBe(false);
      });
    });
  });

  describe('validateCurrency', () => {
    it('should validate ISO currency codes', () => {
      const validCurrencies = ['USD', 'EUR', 'GBP', 'JPY', 'CHF', 'CAD'];

      validCurrencies.forEach(currency => {
        expect(validationService.validateCurrency(currency)).toBe(true);
      });
    });

    it('should reject invalid currency codes', () => {
      const invalidCurrencies = ['INVALID', 'US', 'USDOLLAR', '123', ''];

      invalidCurrencies.forEach(currency => {
        expect(validationService.validateCurrency(currency)).toBe(false);
      });
    });
  });

  describe('validateAmount', () => {
    it('should validate positive amounts', () => {
      const validAmounts = [
        '100000.00',
        '1000',
        '0.01',
        '999999999.99'
      ];

      validAmounts.forEach(amount => {
        expect(validationService.validateAmount(amount)).toBe(true);
      });
    });

    it('should reject invalid amounts', () => {
      const invalidAmounts = [
        '-1000',
        '0',
        'not-a-number',
        '',
        '1000.123' // Too many decimal places
      ];

      invalidAmounts.forEach(amount => {
        expect(validationService.validateAmount(amount)).toBe(false);
      });
    });
  });

  describe('validateDate', () => {
    it('should validate correct date format', () => {
      const validDates = [
        '20240101',
        '20241231',
        '20240229' // Leap year
      ];

      validDates.forEach(date => {
        expect(validationService.validateDate(date)).toBe(true);
      });
    });

    it('should reject invalid date format', () => {
      const invalidDates = [
        '20241301', // Invalid month
        '20240230', // Invalid day
        '2024011',  // Too short
        '202401011', // Too long
        'not-a-date',
        ''
      ];

      invalidDates.forEach(date => {
        expect(validationService.validateDate(date)).toBe(false);
      });
    });
  });

  describe('validateMessageStructure', () => {
    it('should validate complete MT760 message', () => {
      const message = {
        messageType: 'MT760',
        transactionReference: 'TEST123456',
        issueDate: '20240101',
        expiryDate: '20241231',
        guaranteeAmount: '100000.00',
        currency: 'USD',
        applicantName: 'Test Applicant Company',
        beneficiaryName: 'Test Beneficiary Authority',
        guaranteeText: 'Test guarantee text'
      };

      const result = validationService.validateMessageStructure(message);

      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should detect missing required fields', () => {
      const message = {
        messageType: 'MT760',
        // Missing required fields
      };

      const result = validationService.validateMessageStructure(message);

      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(0);
    });

    it('should validate field lengths', () => {
      const message = {
        messageType: 'MT760',
        transactionReference: 'A'.repeat(17), // Too long
        issueDate: '20240101',
        expiryDate: '20241231',
        guaranteeAmount: '100000.00',
        currency: 'USD',
        applicantName: 'Test Applicant Company',
        beneficiaryName: 'Test Beneficiary Authority',
        guaranteeText: 'Test guarantee text'
      };

      const result = validationService.validateMessageStructure(message);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Transaction reference too long');
    });
  });

  describe('validateBusinessRules', () => {
    it('should validate expiry date is after issue date', () => {
      const message = {
        issueDate: '20240101',
        expiryDate: '20241231'
      };

      const result = validationService.validateBusinessRules(message);

      expect(result.isValid).toBe(true);
    });

    it('should reject expiry date before issue date', () => {
      const message = {
        issueDate: '20241231',
        expiryDate: '20240101'
      };

      const result = validationService.validateBusinessRules(message);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Expiry date must be after issue date');
    });

    it('should validate guarantee amount is within limits', () => {
      const message = {
        guaranteeAmount: '1000000.00',
        currency: 'USD'
      };

      const result = validationService.validateBusinessRules(message);

      expect(result.isValid).toBe(true);
    });

    it('should reject excessive guarantee amount', () => {
      const message = {
        guaranteeAmount: '1000000000.00', // 1 billion
        currency: 'USD'
      };

      const result = validationService.validateBusinessRules(message);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Guarantee amount exceeds maximum limit');
    });
  });
});
