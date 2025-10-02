package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.SwiftMessageType;
import com.interexport.guarantees.entity.enums.SwiftMessageStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a SWIFT message for guarantee operations
 * Supports F7 - SWIFT Integration with MT760, MT765, MT767, MT768, MT769 processing
 */
@Entity
@Table(name = "swift_messages", indexes = {
    @Index(name = "idx_swift_message_reference", columnList = "messageReference"),
    @Index(name = "idx_swift_message_type", columnList = "messageType"),
    @Index(name = "idx_swift_message_status", columnList = "status"),
    @Index(name = "idx_swift_message_received_date", columnList = "receivedDate"),
    @Index(name = "idx_swift_related_guarantee", columnList = "relatedGuaranteeId")
})
public class SwiftMessage extends BaseEntity {

    /**
     * SWIFT message reference (unique identifier from SWIFT network)
     */
    @Column(name = "message_reference", length = 16, unique = true)
    @Size(max = 16)
    private String messageReference;

    /**
     * Type of SWIFT message (MT760, MT765, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @NotNull
    private SwiftMessageType messageType;

    /**
     * Current processing status of the message
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private SwiftMessageStatus status = SwiftMessageStatus.RECEIVED;

    /**
     * Raw SWIFT message content as received
     */
    @Column(name = "raw_message", columnDefinition = "TEXT", nullable = false)
    @NotNull
    private String rawMessage;

    /**
     * Parsed SWIFT fields in JSON format for easy access
     */
    @Column(name = "parsed_fields", columnDefinition = "TEXT")
    private String parsedFields;

    /**
     * Date and time when message was received
     */
    @Column(name = "received_date", nullable = false)
    @NotNull
    private LocalDateTime receivedDate = LocalDateTime.now();

    /**
     * Date and time when message processing started
     */
    @Column(name = "processing_started_date")
    private LocalDateTime processingStartedDate;

    /**
     * Date and time when message processing completed
     */
    @Column(name = "processing_completed_date")
    private LocalDateTime processingCompletedDate;

    /**
     * Sender BIC (Bank Identifier Code)
     */
    @Column(name = "sender_bic", length = 11)
    @Size(max = 11)
    private String senderBic;

    /**
     * Receiver BIC (Bank Identifier Code)
     */
    @Column(name = "receiver_bic", length = 11)
    @Size(max = 11)
    private String receiverBic;

    /**
     * Related guarantee contract (if applicable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_guarantee_id")
    private GuaranteeContract relatedGuarantee;

    /**
     * Related amendment (if message is for amendment processing)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_amendment_id")
    private Amendment relatedAmendment;

    /**
     * Priority level (1-9, where 1 is highest priority)
     */
    @Column(name = "priority")
    private Integer priority = 5;

    /**
     * Processing error messages (if any)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Validation error details (if any)
     */
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    /**
     * Response message generated (MT768/MT769)
     */
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    /**
     * Response message reference
     */
    @Column(name = "response_reference", length = 16)
    @Size(max = 16)
    private String responseReference;

    /**
     * Date when response was sent
     */
    @Column(name = "response_sent_date")
    private LocalDateTime responseSentDate;

    /**
     * Additional processing notes
     */
    @Column(name = "processing_notes", columnDefinition = "TEXT")
    private String processingNotes;

    /**
     * Retry count for failed processing
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * Maximum retry attempts allowed
     */
    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    /**
     * Next retry date (for failed messages)
     */
    @Column(name = "next_retry_date")
    private LocalDateTime nextRetryDate;

    /**
     * Business transaction reference from the message
     */
    @Column(name = "transaction_reference", length = 35)
    @Size(max = 35)
    private String transactionReference;

    /**
     * Message sequence number (for related message chains)
     */
    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    /**
     * Parent message (for response messages)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private SwiftMessage parentMessage;

    /**
     * Child messages (responses generated from this message)
     */
    @OneToMany(mappedBy = "parentMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SwiftMessage> childMessages = new ArrayList<>();

    // Default constructor
    public SwiftMessage() {}

    // Constructor for creating new message
    public SwiftMessage(String rawMessage, SwiftMessageType messageType) {
        this.rawMessage = rawMessage;
        this.messageType = messageType;
        this.status = SwiftMessageStatus.RECEIVED;
        this.receivedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getMessageReference() {
        return messageReference;
    }

    public void setMessageReference(String messageReference) {
        this.messageReference = messageReference;
    }

    public SwiftMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(SwiftMessageType messageType) {
        this.messageType = messageType;
    }

    public SwiftMessageStatus getStatus() {
        return status;
    }

    public void setStatus(SwiftMessageStatus status) {
        this.status = status;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getParsedFields() {
        return parsedFields;
    }

    public void setParsedFields(String parsedFields) {
        this.parsedFields = parsedFields;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }

    public LocalDateTime getProcessingStartedDate() {
        return processingStartedDate;
    }

    public void setProcessingStartedDate(LocalDateTime processingStartedDate) {
        this.processingStartedDate = processingStartedDate;
    }

    public LocalDateTime getProcessingCompletedDate() {
        return processingCompletedDate;
    }

    public void setProcessingCompletedDate(LocalDateTime processingCompletedDate) {
        this.processingCompletedDate = processingCompletedDate;
    }

    public String getSenderBic() {
        return senderBic;
    }

    public void setSenderBic(String senderBic) {
        this.senderBic = senderBic;
    }

    public String getReceiverBic() {
        return receiverBic;
    }

    public void setReceiverBic(String receiverBic) {
        this.receiverBic = receiverBic;
    }

    public GuaranteeContract getRelatedGuarantee() {
        return relatedGuarantee;
    }

    public void setRelatedGuarantee(GuaranteeContract relatedGuarantee) {
        this.relatedGuarantee = relatedGuarantee;
    }

    public Amendment getRelatedAmendment() {
        return relatedAmendment;
    }

    public void setRelatedAmendment(Amendment relatedAmendment) {
        this.relatedAmendment = relatedAmendment;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getResponseReference() {
        return responseReference;
    }

    public void setResponseReference(String responseReference) {
        this.responseReference = responseReference;
    }

    public LocalDateTime getResponseSentDate() {
        return responseSentDate;
    }

    public void setResponseSentDate(LocalDateTime responseSentDate) {
        this.responseSentDate = responseSentDate;
    }

    public String getProcessingNotes() {
        return processingNotes;
    }

    public void setProcessingNotes(String processingNotes) {
        this.processingNotes = processingNotes;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public LocalDateTime getNextRetryDate() {
        return nextRetryDate;
    }

    public void setNextRetryDate(LocalDateTime nextRetryDate) {
        this.nextRetryDate = nextRetryDate;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public SwiftMessage getParentMessage() {
        return parentMessage;
    }

    public void setParentMessage(SwiftMessage parentMessage) {
        this.parentMessage = parentMessage;
    }

    public List<SwiftMessage> getChildMessages() {
        return childMessages;
    }

    public void setChildMessages(List<SwiftMessage> childMessages) {
        this.childMessages = childMessages;
    }

    // Utility methods
    public boolean canRetry() {
        return retryCount < maxRetries && status.isError();
    }

    public void incrementRetryCount() {
        this.retryCount = (retryCount == null ? 0 : retryCount) + 1;
    }

    public boolean isProcessingComplete() {
        return status.isCompleted() || (status.isError() && !canRetry());
    }

    @Override
    public String toString() {
        return "SwiftMessage{" +
                "id=" + getId() +
                ", messageReference='" + messageReference + '\'' +
                ", messageType=" + messageType +
                ", status=" + status +
                ", receivedDate=" + receivedDate +
                '}';
    }
}




