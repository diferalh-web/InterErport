const { v4: uuidv4 } = require('uuid');
const moment = require('moment');

class SwiftMessageProcessor {
  constructor() {
    this.messageTemplates = this.initializeTemplates();
    this.responseRules = this.initializeResponseRules();
  }

  /**
   * Process outgoing SWIFT message
   */
  async processOutgoingMessage(messageData) {
    const processed = {
      ...messageData,
      direction: 'OUTGOING',
      rawMessage: this.generateRawSwiftMessage(messageData),
      fields: this.parseMessageFields(messageData.type, messageData.content),
      processed: true,
      processingTime: moment().toISOString()
    };

    return processed;
  }

  /**
   * Process incoming SWIFT message
   */
  async processIncomingMessage(messageData) {
    const processed = {
      ...messageData,
      direction: 'INCOMING',
      fields: this.parseMessageFields(messageData.type, messageData.content),
      processed: true,
      processingTime: moment().toISOString()
    };

    return processed;
  }

  /**
   * Parse incoming raw SWIFT message
   */
  parseIncomingMessage(rawMessage, senderBIC) {
    // Extract basic SWIFT message structure
    const messageType = this.extractMessageType(rawMessage);
    const fields = this.extractMessageFields(rawMessage);
    
    return {
      id: uuidv4(),
      type: messageType,
      content: fields,
      senderBIC: senderBIC,
      receiverBIC: this.extractReceiverBIC(rawMessage),
      timestamp: moment().toISOString(),
      status: 'RECEIVED',
      rawMessage: rawMessage
    };
  }

  /**
   * Generate response based on incoming message
   */
  async generateResponse(originalMessage) {
    const responseRule = this.responseRules[originalMessage.type];
    
    if (!responseRule) {
      return null;
    }

    const responseType = responseRule.responseType;
    const responseTemplate = this.messageTemplates[responseType];

    const responseMessage = {
      id: uuidv4(),
      type: responseType,
      content: this.generateResponseContent(originalMessage, responseTemplate),
      senderBIC: originalMessage.receiverBIC,
      receiverBIC: originalMessage.senderBIC,
      timestamp: moment().toISOString(),
      status: 'SENT',
      direction: 'OUTGOING',
      relatedMessageId: originalMessage.id,
      isResponse: true
    };

    responseMessage.rawMessage = this.generateRawSwiftMessage(responseMessage);
    return responseMessage;
  }

  /**
   * Simulate complex business scenarios
   */
  async simulateScenario(scenarioName, parameters) {
    const scenarios = {
      'guarantee-issuance': this.simulateGuaranteeIssuance,
      'guarantee-amendment': this.simulateGuaranteeAmendment,
      'guarantee-expiry': this.simulateGuaranteeExpiry,
      'claim-process': this.simulateClaimProcess,
      'discrepancy-handling': this.simulateDiscrepancyHandling
    };

    const scenarioFunction = scenarios[scenarioName];
    if (!scenarioFunction) {
      throw new Error(`Unknown scenario: ${scenarioName}`);
    }

    return await scenarioFunction.call(this, parameters);
  }

  /**
   * Simulate guarantee issuance scenario
   */
  async simulateGuaranteeIssuance(parameters) {
    const { guaranteeAmount, currency, beneficiary, applicant } = parameters;
    const messages = [];
    const timeline = [];

    // 1. MT760 - Issue of Guarantee
    const mt760 = {
      id: uuidv4(),
      type: 'MT760',
      direction: 'OUTGOING',
      senderBIC: 'INTEXPORXXXX',
      receiverBIC: beneficiary.bankBIC || 'BNKSGSGXXXX',
      timestamp: moment().toISOString(),
      status: 'SENT',
      content: {
        transactionReference: `GTR${Date.now()}`,
        guaranteeAmount: guaranteeAmount,
        currency: currency,
        issueDate: moment().format('YYYY-MM-DD'),
        expiryDate: moment().add(1, 'year').format('YYYY-MM-DD'),
        applicantName: applicant.name,
        beneficiaryName: beneficiary.name,
        guaranteeText: 'Performance Guarantee as per contract terms'
      }
    };
    
    mt760.rawMessage = this.generateRawSwiftMessage(mt760);
    messages.push(mt760);
    timeline.push({ time: 0, action: 'MT760 sent - Guarantee issued' });

    // 2. MT768 - Acknowledgment (after 2 seconds)
    const mt768 = {
      id: uuidv4(),
      type: 'MT768',
      direction: 'INCOMING',
      senderBIC: beneficiary.bankBIC || 'BNKSGSGXXXX',
      receiverBIC: 'INTEXPORXXXX',
      timestamp: moment().add(2, 'seconds').toISOString(),
      status: 'RECEIVED',
      relatedMessageId: mt760.id,
      content: {
        originalReference: mt760.content.transactionReference,
        acknowledgmentType: 'RECEIVED',
        acknowledgmentText: 'Guarantee received and acknowledged'
      }
    };
    
    mt768.rawMessage = this.generateRawSwiftMessage(mt768);
    messages.push(mt768);
    timeline.push({ time: 2000, action: 'MT768 received - Acknowledgment' });

    return {
      messages,
      timeline,
      summary: {
        scenario: 'guarantee-issuance',
        messagesExchanged: messages.length,
        guaranteeReference: mt760.content.transactionReference,
        status: 'COMPLETED'
      }
    };
  }

  /**
   * Simulate guarantee amendment scenario
   */
  async simulateGuaranteeAmendment(parameters) {
    const { originalGuarantee, amendmentType, newValue } = parameters;
    const messages = [];
    const timeline = [];

    // MT765 - Amendment request
    const mt765 = {
      id: uuidv4(),
      type: 'MT765',
      direction: 'OUTGOING',
      senderBIC: 'INTEXPORXXXX',
      receiverBIC: originalGuarantee.beneficiaryBIC,
      timestamp: moment().toISOString(),
      status: 'SENT',
      content: {
        amendmentReference: `AMD${Date.now()}`,
        originalReference: originalGuarantee.reference,
        amendmentType: amendmentType,
        newValue: newValue,
        amendmentReason: 'Contract modification as requested'
      }
    };

    mt765.rawMessage = this.generateRawSwiftMessage(mt765);
    messages.push(mt765);
    timeline.push({ time: 0, action: 'MT765 sent - Amendment request' });

    // MT767 - Amendment confirmation
    const mt767 = {
      id: uuidv4(),
      type: 'MT767',
      direction: 'INCOMING',
      senderBIC: originalGuarantee.beneficiaryBIC,
      receiverBIC: 'INTEXPORXXXX',
      timestamp: moment().add(5, 'seconds').toISOString(),
      status: 'RECEIVED',
      relatedMessageId: mt765.id,
      content: {
        amendmentReference: mt765.content.amendmentReference,
        confirmationType: 'ACCEPTED',
        confirmationText: 'Amendment accepted and processed'
      }
    };

    mt767.rawMessage = this.generateRawSwiftMessage(mt767);
    messages.push(mt767);
    timeline.push({ time: 5000, action: 'MT767 received - Amendment confirmed' });

    return {
      messages,
      timeline,
      summary: {
        scenario: 'guarantee-amendment',
        messagesExchanged: messages.length,
        amendmentReference: mt765.content.amendmentReference,
        status: 'COMPLETED'
      }
    };
  }

  /**
   * Simulate claim process scenario
   */
  async simulateClaimProcess(parameters) {
    const { guaranteeReference, claimAmount, claimReason } = parameters;
    const messages = [];
    const timeline = [];

    // MT769 - Discrepancy/Claim advice
    const mt769 = {
      id: uuidv4(),
      type: 'MT769',
      direction: 'INCOMING',
      senderBIC: 'BNKSGSGXXXX',
      receiverBIC: 'INTEXPORXXXX',
      timestamp: moment().toISOString(),
      status: 'RECEIVED',
      content: {
        claimReference: `CLM${Date.now()}`,
        originalReference: guaranteeReference,
        claimAmount: claimAmount,
        claimReason: claimReason,
        documentsRequired: ['Invoice', 'Delivery Receipt', 'Contract']
      }
    };

    mt769.rawMessage = this.generateRawSwiftMessage(mt769);
    messages.push(mt769);
    timeline.push({ time: 0, action: 'MT769 received - Claim submitted' });

    return {
      messages,
      timeline,
      summary: {
        scenario: 'claim-process',
        messagesExchanged: messages.length,
        claimReference: mt769.content.claimReference,
        status: 'PENDING_REVIEW'
      }
    };
  }

  /**
   * Initialize message templates
   */
  initializeTemplates() {
    return {
      'MT760': {
        name: 'Issue of a Guarantee',
        fields: ['20', '23', '31C', '31E', '32B', '50', '59', '77C'],
        description: 'Used to issue a new guarantee'
      },
      'MT765': {
        name: 'Amendment to a Guarantee',
        fields: ['20', '21', '31E', '32B', '77A'],
        description: 'Used to request amendment to existing guarantee'
      },
      'MT767': {
        name: 'Confirmation of Amendment',
        fields: ['20', '21', '77A'],
        description: 'Confirmation of guarantee amendment'
      },
      'MT768': {
        name: 'Acknowledgment',
        fields: ['20', '21', '77A'],
        description: 'Acknowledgment of received message'
      },
      'MT769': {
        name: 'Advice of Discrepancy',
        fields: ['20', '21', '32B', '77A'],
        description: 'Advice of discrepancy or claim'
      },
      'MT798': {
        name: 'Free Format Message',
        fields: ['20', '77A'],
        description: 'Free format proprietary message'
      }
    };
  }

  /**
   * Initialize response rules
   */
  initializeResponseRules() {
    return {
      'MT760': { responseType: 'MT768', delay: 2000 },
      'MT765': { responseType: 'MT767', delay: 5000 },
      'MT767': { responseType: 'MT768', delay: 1000 },
      'MT769': { responseType: 'MT768', delay: 3000 }
    };
  }

  /**
   * Generate raw SWIFT message format
   */
  generateRawSwiftMessage(messageData) {
    const { type, senderBIC, receiverBIC, content } = messageData;
    const messageTypeCode = type.substring(2); // Remove 'MT' prefix

    let swiftMessage = `{1:F01${senderBIC}0000000000}`;
    swiftMessage += `{2:I${messageTypeCode}${receiverBIC}N}`;
    swiftMessage += `{3:{108:${type}}}`;
    swiftMessage += `{4:\n`;

    // Add fields based on content
    if (content.transactionReference) {
      swiftMessage += `:20:${content.transactionReference}\n`;
    }
    if (content.originalReference || content.relatedReference) {
      swiftMessage += `:21:${content.originalReference || content.relatedReference}\n`;
    }
    if (content.issueDate) {
      swiftMessage += `:31C:${content.issueDate.replace(/-/g, '')}\n`;
    }
    if (content.expiryDate) {
      swiftMessage += `:31E:${content.expiryDate.replace(/-/g, '')}\n`;
    }
    if (content.guaranteeAmount && content.currency) {
      swiftMessage += `:32B:${content.currency}${content.guaranteeAmount}\n`;
    }
    if (content.applicantName) {
      swiftMessage += `:50:${content.applicantName}\n`;
    }
    if (content.beneficiaryName) {
      swiftMessage += `:59:${content.beneficiaryName}\n`;
    }
    if (content.guaranteeText || content.amendmentReason || content.acknowledgmentText || content.claimReason) {
      const text = content.guaranteeText || content.amendmentReason || content.acknowledgmentText || content.claimReason;
      swiftMessage += `:77C:${text}\n`;
    }

    swiftMessage += `-}{5:{CHK:${this.generateChecksum()}}}`;

    return swiftMessage;
  }

  /**
   * Parse message fields from content
   */
  parseMessageFields(messageType, content) {
    const fields = {};
    
    Object.keys(content).forEach(key => {
      switch(key) {
        case 'transactionReference':
          fields['20'] = content[key];
          break;
        case 'originalReference':
        case 'relatedReference':
          fields['21'] = content[key];
          break;
        case 'issueDate':
          fields['31C'] = content[key];
          break;
        case 'expiryDate':
          fields['31E'] = content[key];
          break;
        case 'guaranteeAmount':
        case 'claimAmount':
          fields['32B'] = `${content.currency || 'USD'}${content[key]}`;
          break;
        case 'applicantName':
          fields['50'] = content[key];
          break;
        case 'beneficiaryName':
          fields['59'] = content[key];
          break;
        default:
          fields[key] = content[key];
      }
    });

    return fields;
  }

  /**
   * Extract message type from raw SWIFT message
   */
  extractMessageType(rawMessage) {
    const match = rawMessage.match(/{2:I(\d{3})/);
    return match ? `MT${match[1]}` : 'UNKNOWN';
  }

  /**
   * Extract receiver BIC from raw message
   */
  extractReceiverBIC(rawMessage) {
    const match = rawMessage.match(/{2:I\d{3}([A-Z]{8}[A-Z0-9]{3})/);
    return match ? match[1] : 'UNKNOWN';
  }

  /**
   * Extract message fields from raw SWIFT message
   */
  extractMessageFields(rawMessage) {
    const fields = {};
    const fieldMatches = rawMessage.match(/:(\d{2}[A-Z]?):(.*?)(?=\n:|$)/g);
    
    if (fieldMatches) {
      fieldMatches.forEach(match => {
        const [, tag, value] = match.match(/:(\d{2}[A-Z]?):(.*)/);
        fields[tag] = value.trim();
      });
    }

    return fields;
  }

  /**
   * Generate response content based on original message
   */
  generateResponseContent(originalMessage, template) {
    const content = {};
    
    if (originalMessage.content.transactionReference) {
      content.originalReference = originalMessage.content.transactionReference;
    }
    
    content.transactionReference = `RSP${Date.now()}`;
    
    switch (originalMessage.type) {
      case 'MT760':
        content.acknowledgmentType = 'RECEIVED';
        content.acknowledgmentText = 'Guarantee received and processed';
        break;
      case 'MT765':
        content.confirmationType = 'ACCEPTED';
        content.confirmationText = 'Amendment accepted';
        break;
      case 'MT769':
        content.acknowledgmentType = 'RECEIVED';
        content.acknowledgmentText = 'Claim received and under review';
        break;
    }

    return content;
  }

  /**
   * Generate checksum for SWIFT message
   */
  generateChecksum() {
    return Math.random().toString(36).substring(2, 14).toUpperCase();
  }

  /**
   * Get available message templates
   */
  getMessageTemplates() {
    return this.messageTemplates;
  }
}

module.exports = SwiftMessageProcessor;




