package com.interexport.guarantees.controller;

import com.interexport.guarantees.entity.SwiftMessage;
import com.interexport.guarantees.entity.enums.SwiftMessageType;
import com.interexport.guarantees.entity.enums.SwiftMessageStatus;
import com.interexport.guarantees.service.SwiftMessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for SWIFT message operations
 * Supports F7 - SWIFT Integration with comprehensive message management API
 */
@RestController
@RequestMapping("/api/v1/swift-messages")
@CrossOrigin(origins = "*")
public class SwiftMessageController {

    private static final Logger logger = LoggerFactory.getLogger(SwiftMessageController.class);
    
    private final SwiftMessageService swiftMessageService;
    
    @Autowired
    public SwiftMessageController(SwiftMessageService swiftMessageService) {
        this.swiftMessageService = swiftMessageService;
    }

    /**
     * Receive incoming SWIFT message
     */
    @PostMapping("/receive")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SWIFT_OPERATOR')")
    public ResponseEntity<SwiftMessageResponse> receiveMessage(@Valid @RequestBody SwiftMessageRequest request) {
        try {
            logger.info("Receiving SWIFT message of type: {}", request.getMessageType());
            
            SwiftMessageType messageType = SwiftMessageType.valueOf(request.getMessageType());
            SwiftMessage message = swiftMessageService.receiveMessage(request.getRawMessage(), messageType);
            
            SwiftMessageResponse response = new SwiftMessageResponse();
            response.setId(message.getId());
            response.setStatus(message.getStatus().toString());
            response.setMessageReference(message.getMessageReference());
            response.setReceivedDate(message.getReceivedDate());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error receiving SWIFT message: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all SWIFT messages with pagination
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<SwiftMessageDto>> getAllMessages(Pageable pageable) {
        try {
            Page<SwiftMessage> messages = swiftMessageService.getMessages(pageable);
            Page<SwiftMessageDto> dtos = messages.map(this::convertToDto);
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching SWIFT messages: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get SWIFT message by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SwiftMessageDto> getMessageById(@PathVariable Long id) {
        try {
            Optional<SwiftMessage> messageOpt = swiftMessageService.getMessageById(id);
            
            if (messageOpt.isPresent()) {
                SwiftMessageDto dto = convertToDto(messageOpt.get());
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching SWIFT message {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get messages by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<SwiftMessageDto>> getMessagesByStatus(
            @PathVariable String status, 
            Pageable pageable) {
        try {
            SwiftMessageStatus messageStatus = SwiftMessageStatus.valueOf(status.toUpperCase());
            Page<SwiftMessage> messages = swiftMessageService.getMessagesByStatus(messageStatus, pageable);
            Page<SwiftMessageDto> dtos = messages.map(this::convertToDto);
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching messages by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get messages for specific guarantee
     */
    @GetMapping("/guarantee/{guaranteeId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SwiftMessageDto>> getMessagesForGuarantee(@PathVariable Long guaranteeId) {
        try {
            List<SwiftMessage> messages = swiftMessageService.getMessagesForGuarantee(guaranteeId);
            List<SwiftMessageDto> dtos = messages.stream()
                    .map(this::convertToDto)
                    .toList();
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching messages for guarantee {}: {}", guaranteeId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get messages pending processing
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SWIFT_OPERATOR')")
    public ResponseEntity<List<SwiftMessageDto>> getPendingMessages() {
        try {
            List<SwiftMessage> messages = swiftMessageService.getMessagesForProcessing();
            List<SwiftMessageDto> dtos = messages.stream()
                    .map(this::convertToDto)
                    .toList();
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching pending messages: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retry failed message processing
     */
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SWIFT_OPERATOR')")
    public ResponseEntity<Void> retryMessage(@PathVariable Long id) {
        try {
            swiftMessageService.retryMessage(id);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error retrying message {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Reprocess message manually
     */
    @PostMapping("/{id}/reprocess")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reprocessMessage(@PathVariable Long id) {
        try {
            swiftMessageService.processMessageAsync(id);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error reprocessing message {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get message statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SwiftMessageStatistics> getStatistics() {
        // Implementation would include counts by status, type, etc.
        SwiftMessageStatistics stats = new SwiftMessageStatistics();
        // This would be populated with actual statistics
        return ResponseEntity.ok(stats);
    }

    // Utility method to convert entity to DTO
    private SwiftMessageDto convertToDto(SwiftMessage message) {
        SwiftMessageDto dto = new SwiftMessageDto();
        dto.setId(message.getId());
        dto.setMessageReference(message.getMessageReference());
        dto.setMessageType(message.getMessageType().toString());
        dto.setStatus(message.getStatus().toString());
        dto.setReceivedDate(message.getReceivedDate());
        dto.setProcessingStartedDate(message.getProcessingStartedDate());
        dto.setProcessingCompletedDate(message.getProcessingCompletedDate());
        dto.setSenderBic(message.getSenderBic());
        dto.setReceiverBic(message.getReceiverBic());
        dto.setTransactionReference(message.getTransactionReference());
        dto.setErrorMessage(message.getErrorMessage());
        dto.setValidationErrors(message.getValidationErrors());
        dto.setProcessingNotes(message.getProcessingNotes());
        dto.setRetryCount(message.getRetryCount());
        dto.setMaxRetries(message.getMaxRetries());
        dto.setNextRetryDate(message.getNextRetryDate());
        dto.setResponseReference(message.getResponseReference());
        dto.setResponseSentDate(message.getResponseSentDate());
        
        if (message.getRelatedGuarantee() != null) {
            dto.setRelatedGuaranteeId(message.getRelatedGuarantee().getId());
            dto.setRelatedGuaranteeReference(message.getRelatedGuarantee().getReference());
        }
        
        if (message.getRelatedAmendment() != null) {
            dto.setRelatedAmendmentId(message.getRelatedAmendment().getId());
            dto.setRelatedAmendmentReference(message.getRelatedAmendment().getAmendmentReference());
        }
        
        return dto;
    }

    // Request/Response classes
    public static class SwiftMessageRequest {
        private String rawMessage;
        private String messageType;
        
        public String getRawMessage() { return rawMessage; }
        public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
    }

    public static class SwiftMessageResponse {
        private Long id;
        private String status;
        private String messageReference;
        private java.time.LocalDateTime receivedDate;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessageReference() { return messageReference; }
        public void setMessageReference(String messageReference) { this.messageReference = messageReference; }
        
        public java.time.LocalDateTime getReceivedDate() { return receivedDate; }
        public void setReceivedDate(java.time.LocalDateTime receivedDate) { this.receivedDate = receivedDate; }
    }

    public static class SwiftMessageDto {
        private Long id;
        private String messageReference;
        private String messageType;
        private String status;
        private java.time.LocalDateTime receivedDate;
        private java.time.LocalDateTime processingStartedDate;
        private java.time.LocalDateTime processingCompletedDate;
        private String senderBic;
        private String receiverBic;
        private String transactionReference;
        private String errorMessage;
        private String validationErrors;
        private String processingNotes;
        private Integer retryCount;
        private Integer maxRetries;
        private java.time.LocalDateTime nextRetryDate;
        private String responseReference;
        private java.time.LocalDateTime responseSentDate;
        private Long relatedGuaranteeId;
        private String relatedGuaranteeReference;
        private Long relatedAmendmentId;
        private String relatedAmendmentReference;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getMessageReference() { return messageReference; }
        public void setMessageReference(String messageReference) { this.messageReference = messageReference; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public java.time.LocalDateTime getReceivedDate() { return receivedDate; }
        public void setReceivedDate(java.time.LocalDateTime receivedDate) { this.receivedDate = receivedDate; }
        
        public java.time.LocalDateTime getProcessingStartedDate() { return processingStartedDate; }
        public void setProcessingStartedDate(java.time.LocalDateTime processingStartedDate) { this.processingStartedDate = processingStartedDate; }
        
        public java.time.LocalDateTime getProcessingCompletedDate() { return processingCompletedDate; }
        public void setProcessingCompletedDate(java.time.LocalDateTime processingCompletedDate) { this.processingCompletedDate = processingCompletedDate; }
        
        public String getSenderBic() { return senderBic; }
        public void setSenderBic(String senderBic) { this.senderBic = senderBic; }
        
        public String getReceiverBic() { return receiverBic; }
        public void setReceiverBic(String receiverBic) { this.receiverBic = receiverBic; }
        
        public String getTransactionReference() { return transactionReference; }
        public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getValidationErrors() { return validationErrors; }
        public void setValidationErrors(String validationErrors) { this.validationErrors = validationErrors; }
        
        public String getProcessingNotes() { return processingNotes; }
        public void setProcessingNotes(String processingNotes) { this.processingNotes = processingNotes; }
        
        public Integer getRetryCount() { return retryCount; }
        public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
        
        public Integer getMaxRetries() { return maxRetries; }
        public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
        
        public java.time.LocalDateTime getNextRetryDate() { return nextRetryDate; }
        public void setNextRetryDate(java.time.LocalDateTime nextRetryDate) { this.nextRetryDate = nextRetryDate; }
        
        public String getResponseReference() { return responseReference; }
        public void setResponseReference(String responseReference) { this.responseReference = responseReference; }
        
        public java.time.LocalDateTime getResponseSentDate() { return responseSentDate; }
        public void setResponseSentDate(java.time.LocalDateTime responseSentDate) { this.responseSentDate = responseSentDate; }
        
        public Long getRelatedGuaranteeId() { return relatedGuaranteeId; }
        public void setRelatedGuaranteeId(Long relatedGuaranteeId) { this.relatedGuaranteeId = relatedGuaranteeId; }
        
        public String getRelatedGuaranteeReference() { return relatedGuaranteeReference; }
        public void setRelatedGuaranteeReference(String relatedGuaranteeReference) { this.relatedGuaranteeReference = relatedGuaranteeReference; }
        
        public Long getRelatedAmendmentId() { return relatedAmendmentId; }
        public void setRelatedAmendmentId(Long relatedAmendmentId) { this.relatedAmendmentId = relatedAmendmentId; }
        
        public String getRelatedAmendmentReference() { return relatedAmendmentReference; }
        public void setRelatedAmendmentReference(String relatedAmendmentReference) { this.relatedAmendmentReference = relatedAmendmentReference; }
    }

    public static class SwiftMessageStatistics {
        private long totalMessages;
        private long pendingMessages;
        private long processedMessages;
        private long errorMessages;
        
        // Getters and setters
        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
        
        public long getPendingMessages() { return pendingMessages; }
        public void setPendingMessages(long pendingMessages) { this.pendingMessages = pendingMessages; }
        
        public long getProcessedMessages() { return processedMessages; }
        public void setProcessedMessages(long processedMessages) { this.processedMessages = processedMessages; }
        
        public long getErrorMessages() { return errorMessages; }
        public void setErrorMessages(long errorMessages) { this.errorMessages = errorMessages; }
    }
}


