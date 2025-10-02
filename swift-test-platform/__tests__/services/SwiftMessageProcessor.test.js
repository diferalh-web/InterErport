const SwiftMessageProcessor = require('../../services/SwiftMessageProcessor');

describe('SwiftMessageProcessor', () => {
  let processor;

  beforeEach(() => {
    processor = new SwiftMessageProcessor();
  });

  describe('parseMessage', () => {
    it('should parse valid MT760 message', () => {
      const rawMessage = `{1:F01TESTBANKXXX0000000000}{2:I760INTEXPORXXXXN}{3:{108:MT760}}{4:
:20:TEST123456
:31C:20240101
:31E:20241231
:32B:USD100000,00
:50:Test Applicant Company
:59:Test Beneficiary Authority
:77C:TEST PERFORMANCE GUARANTEE
-}{5:{CHK:ABCDEF123456}}`;

      const result = processor.parseMessage(rawMessage);

      expect(result).toBeDefined();
      expect(result.messageType).toBe('MT760');
      expect(result.transactionReference).toBe('TEST123456');
      expect(result.guaranteeAmount).toBe('100000.00');
      expect(result.currency).toBe('USD');
    });

    it('should handle invalid message format', () => {
      const invalidMessage = 'Invalid SWIFT message format';

      expect(() => {
        processor.parseMessage(invalidMessage);
      }).toThrow('Invalid SWIFT message format');
    });

    it('should parse MT765 amendment message', () => {
      const rawMessage = `{1:F01TESTBANKXXX0000000000}{2:I765INTEXPORXXXXN}{3:{108:MT765}}{4:
:20:AMEND123456
:21:ORIG123456
:31C:20240101
:32B:USD150000,00
:77C:AMENDMENT TO GUARANTEE
-}{5:{CHK:ABCDEF123456}}`;

      const result = processor.parseMessage(rawMessage);

      expect(result.messageType).toBe('MT765');
      expect(result.originalReference).toBe('ORIG123456');
      expect(result.guaranteeAmount).toBe('150000.00');
    });
  });

  describe('validateMessage', () => {
    it('should validate correct message structure', () => {
      const message = {
        messageType: 'MT760',
        transactionReference: 'TEST123',
        guaranteeAmount: '100000.00',
        currency: 'USD',
        applicantName: 'Test Applicant',
        beneficiaryName: 'Test Beneficiary'
      };

      const result = processor.validateMessage(message);

      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should detect missing required fields', () => {
      const message = {
        messageType: 'MT760',
        // Missing required fields
      };

      const result = processor.validateMessage(message);

      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.errors).toContain('Transaction reference is required');
      expect(result.errors).toContain('Guarantee amount is required');
    });

    it('should validate currency codes', () => {
      const message = {
        messageType: 'MT760',
        transactionReference: 'TEST123',
        guaranteeAmount: '100000.00',
        currency: 'INVALID',
        applicantName: 'Test Applicant',
        beneficiaryName: 'Test Beneficiary'
      };

      const result = processor.validateMessage(message);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Invalid currency code: INVALID');
    });

    it('should validate amount format', () => {
      const message = {
        messageType: 'MT760',
        transactionReference: 'TEST123',
        guaranteeAmount: 'not-a-number',
        currency: 'USD',
        applicantName: 'Test Applicant',
        beneficiaryName: 'Test Beneficiary'
      };

      const result = processor.validateMessage(message);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContain('Invalid amount format');
    });
  });

  describe('generateResponse', () => {
    it('should generate MT768 acknowledgment', () => {
      const originalMessage = {
        messageType: 'MT760',
        transactionReference: 'TEST123',
        senderBIC: 'TESTBANKXXX',
        receiverBIC: 'INTEXPORXXXX'
      };

      const response = processor.generateResponse(originalMessage, 'MT768');

      expect(response.messageType).toBe('MT768');
      expect(response.relatedMessageId).toBe(originalMessage.transactionReference);
      expect(response.senderBIC).toBe(originalMessage.receiverBIC);
      expect(response.receiverBIC).toBe(originalMessage.senderBIC);
    });

    it('should generate MT769 discrepancy notice', () => {
      const originalMessage = {
        messageType: 'MT760',
        transactionReference: 'TEST123',
        senderBIC: 'TESTBANKXXX',
        receiverBIC: 'INTEXPORXXXX'
      };

      const response = processor.generateResponse(originalMessage, 'MT769', 'Invalid amount format');

      expect(response.messageType).toBe('MT769');
      expect(response.relatedMessageId).toBe(originalMessage.transactionReference);
      expect(response.discrepancyReason).toBe('Invalid amount format');
    });
  });

  describe('formatMessage', () => {
    it('should format message for transmission', () => {
      const message = {
        messageType: 'MT760',
        transactionReference: 'TEST123',
        guaranteeAmount: '100000.00',
        currency: 'USD',
        applicantName: 'Test Applicant',
        beneficiaryName: 'Test Beneficiary'
      };

      const formatted = processor.formatMessage(message);

      expect(formatted).toContain('{1:');
      expect(formatted).toContain('{2:I760');
      expect(formatted).toContain(':20:TEST123');
      expect(formatted).toContain(':32B:USD100000,00');
    });
  });
});
