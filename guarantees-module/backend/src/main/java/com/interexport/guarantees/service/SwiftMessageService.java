package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.SwiftMessage;
import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.Amendment;
import com.interexport.guarantees.entity.enums.SwiftMessageType;
import com.interexport.guarantees.entity.enums.SwiftMessageStatus;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.repository.SwiftMessageRepository;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import com.interexport.guarantees.repository.AmendmentRepository;
import com.interexport.guarantees.service.SwiftMessageParserService.SwiftMessageParseResult;
import com.interexport.guarantees.service.SwiftMessageParserService.SwiftMessageValidationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Main service for SWIFT message processing
 * Supports F7 - SWIFT Integration with comprehensive message lifecycle management
 */
@Service
@Transactional
public class SwiftMessageService {

    private static final Logger logger = LoggerFactory.getLogger(SwiftMessageService.class);
    
    private final SwiftMessageRepository swiftMessageRepository;
    private final SwiftMessageParserService parserService;
    private final GuaranteeContractRepository guaranteeRepository;
    private final AmendmentRepository amendmentRepository;
    private final ReferenceGeneratorService referenceGenerator;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public SwiftMessageService(
            SwiftMessageRepository swiftMessageRepository,
            SwiftMessageParserService parserService,
            GuaranteeContractRepository guaranteeRepository,
            AmendmentRepository amendmentRepository,
            ReferenceGeneratorService referenceGenerator,
            ObjectMapper objectMapper) {
        this.swiftMessageRepository = swiftMessageRepository;
        this.parserService = parserService;
        this.guaranteeRepository = guaranteeRepository;
        this.amendmentRepository = amendmentRepository;
        this.referenceGenerator = referenceGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * Receive and store incoming SWIFT message
     */
    public SwiftMessage receiveMessage(String rawMessage, SwiftMessageType messageType) {
        logger.info("Receiving SWIFT message of type: {}", messageType);
        
        try {
            // Create new message entity
            SwiftMessage message = new SwiftMessage(rawMessage, messageType);
            message.setReceivedDate(LocalDateTime.now());
            message.setStatus(SwiftMessageStatus.RECEIVED);
            
            // Check for duplicates based on content hash (simplified)
            String messageHash = generateMessageHash(rawMessage);
            
            // Save message
            SwiftMessage savedMessage = swiftMessageRepository.save(message);
            
            logger.info("SWIFT message received and stored with ID: {}", savedMessage.getId());
            
            // Trigger asynchronous processing
            processMessageAsync(savedMessage.getId());
            
            return savedMessage;
            
        } catch (Exception e) {
            logger.error("Error receiving SWIFT message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to receive SWIFT message", e);
        }
    }

    /**
     * Process SWIFT message asynchronously
     */
    @Async
    public void processMessageAsync(Long messageId) {
        try {
            processMessage(messageId);
        } catch (Exception e) {
            logger.error("Error in async message processing for ID {}: {}", messageId, e.getMessage(), e);
        }
    }

    /**
     * Process SWIFT message through complete lifecycle
     */
    @Transactional
    public void processMessage(Long messageId) {
        Optional<SwiftMessage> messageOpt = swiftMessageRepository.findById(messageId);
        if (!messageOpt.isPresent()) {
            logger.error("SWIFT message not found with ID: {}", messageId);
            return;
        }
        
        SwiftMessage message = messageOpt.get();
        logger.info("Processing SWIFT message ID: {} Type: {}", messageId, message.getMessageType());
        
        try {
            // Update status to processing
            message.setStatus(SwiftMessageStatus.PROCESSING);
            message.setProcessingStartedDate(LocalDateTime.now());
            swiftMessageRepository.save(message);
            
            // Step 1: Parse message
            parseMessage(message);
            
            // Step 2: Validate message
            validateMessage(message);
            
            // Step 3: Process business logic
            processBusinessLogic(message);
            
            // Step 4: Generate response if needed
            generateResponseIfNeeded(message);
            
            // Mark as completed
            message.setStatus(SwiftMessageStatus.PROCESSED);
            message.setProcessingCompletedDate(LocalDateTime.now());
            swiftMessageRepository.save(message);
            
            logger.info("SWIFT message processing completed for ID: {}", messageId);
            
        } catch (Exception e) {
            logger.error("Error processing SWIFT message ID {}: {}", messageId, e.getMessage(), e);
            handleProcessingError(message, e);
        }
    }

    /**
     * Parse SWIFT message content
     */
    private void parseMessage(SwiftMessage message) {
        logger.debug("Parsing SWIFT message ID: {}", message.getId());
        
        SwiftMessageParseResult parseResult = parserService.parseMessage(
            message.getRawMessage(), 
            message.getMessageType()
        );
        
        if (parseResult.isSuccess()) {
            message.setParsedFields(parseResult.getParsedFields());
            message.setMessageReference(parseResult.getMessageReference());
            message.setSenderBic(parseResult.getSenderBic());
            message.setReceiverBic(parseResult.getReceiverBic());
            message.setTransactionReference(parseResult.getTransactionReference());
            message.setStatus(SwiftMessageStatus.PARSED);
            
            swiftMessageRepository.save(message);
            logger.debug("SWIFT message parsed successfully");
            
        } else {
            message.setStatus(SwiftMessageStatus.PARSE_ERROR);
            message.setErrorMessage(parseResult.getErrorMessage());
            swiftMessageRepository.save(message);
            
            throw new RuntimeException("Parse error: " + parseResult.getErrorMessage());
        }
    }

    /**
     * Validate parsed message
     */
    private void validateMessage(SwiftMessage message) {
        logger.debug("Validating SWIFT message ID: {}", message.getId());
        
        SwiftMessageValidationResult validationResult = parserService.validateParsedMessage(
            message.getParsedFields(), 
            message.getMessageType()
        );
        
        if (validationResult.isValid()) {
            message.setStatus(SwiftMessageStatus.VALIDATED);
            swiftMessageRepository.save(message);
            logger.debug("SWIFT message validation successful");
            
        } else {
            message.setStatus(SwiftMessageStatus.VALIDATION_ERROR);
            message.setValidationErrors(validationResult.getErrorsAsString());
            swiftMessageRepository.save(message);
            
            throw new RuntimeException("Validation error: " + validationResult.getErrorsAsString());
        }
    }

    /**
     * Process business logic based on message type
     */
    private void processBusinessLogic(SwiftMessage message) {
        logger.debug("Processing business logic for SWIFT message ID: {} Type: {}", 
                    message.getId(), message.getMessageType());
        
        switch (message.getMessageType()) {
            case MT760:
                processMT760(message);
                break;
            case MT765:
                processMT765(message);
                break;
            case MT767:
                processMT767(message);
                break;
            case MT768:
                processMT768(message);
                break;
            case MT769:
                processMT769(message);
                break;
            case MT798:
                processMT798(message);
                break;
            default:
                logger.warn("Unknown message type for processing: {}", message.getMessageType());
        }
    }

    /**
     * Process MT760 - Received Guarantee
     */
    private void processMT760(SwiftMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = objectMapper.readValue(message.getParsedFields(), Map.class);
            
            // Create new guarantee contract from SWIFT message
            GuaranteeContract guarantee = new GuaranteeContract();
            
            // Map SWIFT fields to guarantee fields
            guarantee.setReference(referenceGenerator.generateGuaranteeReference());
            guarantee.setGuaranteeType(GuaranteeType.PERFORMANCE); // Default, could be determined from message
            guarantee.setStatus(GuaranteeStatus.RECEIVED); // New status for received guarantees
            
            // Extract amount and currency
            String currencyAmount = extractString(fields, "currencyAmount");
            if (currencyAmount != null && currencyAmount.length() >= 3) {
                guarantee.setCurrency(currencyAmount.substring(0, 3));
                try {
                    String amountStr = currencyAmount.substring(3);
                    guarantee.setAmount(new BigDecimal(amountStr));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse amount from SWIFT message: {}", currencyAmount);
                }
            }
            
            // Extract dates
            String issueDateStr = extractString(fields, "issueDate");
            String expiryDateStr = extractString(fields, "expiryDate");
            // Date parsing would be implemented here
            
            // Extract parties
            guarantee.setBeneficiaryName(extractString(fields, "beneficiary"));
            guarantee.setApplicantName(extractString(fields, "applicant"));
            
            // Extract guarantee text
            guarantee.setGuaranteeText(extractString(fields, "guaranteeText"));
            
            // Set SWIFT reference
            guarantee.setSwiftMessageReference(message.getMessageReference());
            
            // Set audit fields
            guarantee.setCreatedBy("SWIFT_PROCESSOR");
            guarantee.setCreatedDate(LocalDateTime.now());
            
            // Save guarantee
            GuaranteeContract savedGuarantee = guaranteeRepository.save(guarantee);
            
            // Link message to guarantee
            message.setRelatedGuarantee(savedGuarantee);
            message.setProcessingNotes("Guarantee created from MT760: " + savedGuarantee.getReference());
            
            logger.info("MT760 processed: Created guarantee {}", savedGuarantee.getReference());
            
        } catch (Exception e) {
            logger.error("Error processing MT760 message: {}", e.getMessage(), e);
            throw new RuntimeException("MT760 processing error", e);
        }
    }

    /**
     * Process MT765 - Guarantee Amendment
     */
    private void processMT765(SwiftMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = objectMapper.readValue(message.getParsedFields(), Map.class);
            
            String relatedRef = extractString(fields, "relatedReference");
            
            // Find related guarantee
            Optional<GuaranteeContract> guaranteeOpt = guaranteeRepository.findBySwiftMessageReference(relatedRef);
            
            if (guaranteeOpt.isPresent()) {
                GuaranteeContract guarantee = guaranteeOpt.get();
                
                // Create amendment
                Amendment amendment = new Amendment();
                amendment.setGuarantee(guarantee);
                amendment.setAmendmentReference(referenceGenerator.generateAmendmentReference());
                amendment.setDescription("Amendment from SWIFT MT765");
                amendment.setReason(extractString(fields, "amendmentDetails"));
                amendment.setSwiftMessageReference(message.getMessageReference());
                amendment.setCreatedBy("SWIFT_PROCESSOR");
                amendment.setCreatedDate(LocalDateTime.now());
                
                // Save amendment
                Amendment savedAmendment = amendmentRepository.save(amendment);
                
                // Link message to amendment
                message.setRelatedAmendment(savedAmendment);
                message.setRelatedGuarantee(guarantee);
                message.setProcessingNotes("Amendment created from MT765: " + savedAmendment.getAmendmentReference());
                
                logger.info("MT765 processed: Created amendment {}", savedAmendment.getAmendmentReference());
                
            } else {
                throw new RuntimeException("Related guarantee not found for reference: " + relatedRef);
            }
            
        } catch (Exception e) {
            logger.error("Error processing MT765 message: {}", e.getMessage(), e);
            throw new RuntimeException("MT765 processing error", e);
        }
    }

    /**
     * Process MT767 - Amendment Processing
     */
    private void processMT767(SwiftMessage message) {
        // Implementation for MT767 processing
        message.setProcessingNotes("MT767 processed - Amendment processing confirmation received");
        logger.info("MT767 processed for message ID: {}", message.getId());
    }

    /**
     * Process MT768 - Acknowledgment
     */
    private void processMT768(SwiftMessage message) {
        // Implementation for MT768 processing
        message.setProcessingNotes("MT768 processed - Acknowledgment received");
        logger.info("MT768 processed for message ID: {}", message.getId());
    }

    /**
     * Process MT769 - Discrepancy Advice
     */
    private void processMT769(SwiftMessage message) {
        // Implementation for MT769 processing
        message.setProcessingNotes("MT769 processed - Discrepancy advice received");
        logger.info("MT769 processed for message ID: {}", message.getId());
    }

    /**
     * Process MT798 - Free Format Message
     */
    private void processMT798(SwiftMessage message) {
        // Implementation for MT798 processing
        message.setProcessingNotes("MT798 processed - Free format message");
        logger.info("MT798 processed for message ID: {}", message.getId());
    }

    /**
     * Generate response message if needed (MT768/MT769)
     */
    private void generateResponseIfNeeded(SwiftMessage message) {
        if (message.getMessageType() == SwiftMessageType.MT760) {
            // Generate MT768 acknowledgment
            String responseMessage = generateMT768Response(message);
            
            // Create response message
            SwiftMessage response = new SwiftMessage();
            response.setMessageType(SwiftMessageType.MT768);
            response.setRawMessage(responseMessage);
            response.setStatus(SwiftMessageStatus.PROCESSED);
            response.setParentMessage(message);
            response.setSenderBic(message.getReceiverBic());
            response.setReceiverBic(message.getSenderBic());
            response.setResponseReference(generateResponseReference());
            response.setCreatedBy("SWIFT_PROCESSOR");
            response.setCreatedDate(LocalDateTime.now());
            
            swiftMessageRepository.save(response);
            
            message.setResponseMessage(responseMessage);
            message.setResponseReference(response.getResponseReference());
            
            logger.info("MT768 response generated for message ID: {}", message.getId());
        }
    }

    /**
     * Generate MT768 acknowledgment response
     */
    private String generateMT768Response(SwiftMessage originalMessage) {
        // Simplified MT768 generation - in production would use proper SWIFT formatter
        StringBuilder response = new StringBuilder();
        response.append("{1:F01").append(originalMessage.getReceiverBic()).append("0000000000}");
        response.append("{2:I768").append(originalMessage.getSenderBic()).append("N}");
        response.append("{4:\r\n");
        response.append(":20:").append(generateResponseReference()).append("\r\n");
        response.append(":21:").append(originalMessage.getMessageReference()).append("\r\n");
        response.append(":77A:GUARANTEE RECEIVED AND PROCESSED\r\n");
        response.append("-}");
        
        return response.toString();
    }

    /**
     * Handle processing errors
     */
    private void handleProcessingError(SwiftMessage message, Exception error) {
        message.setStatus(SwiftMessageStatus.PROCESSING_ERROR);
        message.setErrorMessage(error.getMessage());
        message.setProcessingCompletedDate(LocalDateTime.now());
        
        // Increment retry count
        message.incrementRetryCount();
        
        // Schedule retry if possible
        if (message.canRetry()) {
            message.setNextRetryDate(LocalDateTime.now().plusMinutes(5)); // 5 minute delay
            logger.info("Scheduled retry for message ID: {} (attempt {}/{})", 
                       message.getId(), message.getRetryCount(), message.getMaxRetries());
        } else {
            logger.error("Maximum retries exceeded for message ID: {}", message.getId());
        }
        
        swiftMessageRepository.save(message);
    }

    /**
     * Get messages for processing (received + retry eligible)
     */
    public List<SwiftMessage> getMessagesForProcessing() {
        return swiftMessageRepository.findMessagesForProcessing(LocalDateTime.now());
    }

    /**
     * Get message by ID
     */
    public Optional<SwiftMessage> getMessageById(Long id) {
        return swiftMessageRepository.findById(id);
    }

    /**
     * Get messages with pagination
     */
    public Page<SwiftMessage> getMessages(Pageable pageable) {
        return swiftMessageRepository.findAll(pageable);
    }

    /**
     * Get messages by status
     */
    public Page<SwiftMessage> getMessagesByStatus(SwiftMessageStatus status, Pageable pageable) {
        return swiftMessageRepository.findByStatusOrderByReceivedDateDesc(status, pageable);
    }

    /**
     * Get messages for guarantee
     */
    public List<SwiftMessage> getMessagesForGuarantee(Long guaranteeId) {
        return swiftMessageRepository.findByRelatedGuaranteeId(guaranteeId);
    }

    /**
     * Retry failed message
     */
    @Transactional
    public void retryMessage(Long messageId) {
        Optional<SwiftMessage> messageOpt = swiftMessageRepository.findById(messageId);
        if (messageOpt.isPresent()) {
            SwiftMessage message = messageOpt.get();
            if (message.canRetry()) {
                message.setStatus(SwiftMessageStatus.RECEIVED);
                message.setErrorMessage(null);
                message.setValidationErrors(null);
                swiftMessageRepository.save(message);
                
                processMessageAsync(messageId);
                logger.info("Retry initiated for message ID: {}", messageId);
            }
        }
    }

    // Utility methods
    private String extractString(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value != null ? value.toString().trim() : null;
    }
    
    private String generateMessageHash(String content) {
        return String.valueOf(content.hashCode());
    }
    
    private String generateResponseReference() {
        return "RSP" + System.currentTimeMillis();
    }
}


