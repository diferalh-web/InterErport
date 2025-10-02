package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.SwiftMessage;
import com.interexport.guarantees.entity.enums.SwiftMessageType;
import com.interexport.guarantees.entity.enums.SwiftMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SwiftMessage entities
 * Supports F7 - SWIFT Integration with comprehensive query methods
 */
@Repository
public interface SwiftMessageRepository extends JpaRepository<SwiftMessage, Long> {

    /**
     * Find SWIFT message by unique message reference
     */
    Optional<SwiftMessage> findByMessageReference(String messageReference);

    /**
     * Find messages by type and status
     */
    List<SwiftMessage> findByMessageTypeAndStatusOrderByReceivedDateDesc(
            SwiftMessageType messageType, 
            SwiftMessageStatus status
    );

    /**
     * Find messages by status with pagination
     */
    Page<SwiftMessage> findByStatusOrderByReceivedDateDesc(
            SwiftMessageStatus status, 
            Pageable pageable
    );

    /**
     * Find messages requiring processing (received or failed with retry available)
     */
    @Query("SELECT m FROM SwiftMessage m WHERE " +
           "m.status = 'RECEIVED' OR " +
           "(m.status IN ('PARSE_ERROR', 'VALIDATION_ERROR', 'PROCESSING_ERROR') " +
           "AND m.retryCount < m.maxRetries AND (m.nextRetryDate IS NULL OR m.nextRetryDate <= :now))")
    List<SwiftMessage> findMessagesForProcessing(@Param("now") LocalDateTime now);

    /**
     * Find messages by sender BIC
     */
    List<SwiftMessage> findBySenderBicOrderByReceivedDateDesc(String senderBic);

    /**
     * Find messages related to a specific guarantee
     */
    @Query("SELECT m FROM SwiftMessage m WHERE m.relatedGuarantee.id = :guaranteeId " +
           "ORDER BY m.receivedDate DESC")
    List<SwiftMessage> findByRelatedGuaranteeId(@Param("guaranteeId") Long guaranteeId);

    /**
     * Find messages related to a specific amendment
     */
    @Query("SELECT m FROM SwiftMessage m WHERE m.relatedAmendment.id = :amendmentId " +
           "ORDER BY m.receivedDate DESC")
    List<SwiftMessage> findByRelatedAmendmentId(@Param("amendmentId") Long amendmentId);

    /**
     * Find messages by transaction reference (business reference)
     */
    List<SwiftMessage> findByTransactionReferenceOrderBySequenceNumberAsc(String transactionReference);

    /**
     * Find messages received within date range
     */
    @Query("SELECT m FROM SwiftMessage m WHERE m.receivedDate >= :startDate AND m.receivedDate <= :endDate " +
           "ORDER BY m.receivedDate DESC")
    List<SwiftMessage> findByReceivedDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find messages by multiple statuses
     */
    List<SwiftMessage> findByStatusInOrderByReceivedDateDesc(List<SwiftMessageStatus> statuses);

    /**
     * Find error messages with retry available
     */
    @Query("SELECT m FROM SwiftMessage m WHERE " +
           "m.status IN ('PARSE_ERROR', 'VALIDATION_ERROR', 'PROCESSING_ERROR') " +
           "AND m.retryCount < m.maxRetries " +
           "ORDER BY m.receivedDate DESC")
    List<SwiftMessage> findRetryableErrorMessages();

    /**
     * Find messages ready for retry
     */
    @Query("SELECT m FROM SwiftMessage m WHERE " +
           "m.status IN ('PARSE_ERROR', 'VALIDATION_ERROR', 'PROCESSING_ERROR') " +
           "AND m.retryCount < m.maxRetries " +
           "AND (m.nextRetryDate IS NULL OR m.nextRetryDate <= :now) " +
           "ORDER BY m.priority ASC, m.receivedDate ASC")
    List<SwiftMessage> findMessagesReadyForRetry(@Param("now") LocalDateTime now);

    /**
     * Find child messages (responses) for a parent message
     */
    List<SwiftMessage> findByParentMessageOrderByCreatedDateAsc(SwiftMessage parentMessage);

    /**
     * Count messages by status
     */
    long countByStatus(SwiftMessageStatus status);

    /**
     * Count messages by type and status
     */
    long countByMessageTypeAndStatus(SwiftMessageType messageType, SwiftMessageStatus status);

    /**
     * Count messages received today
     */
    @Query("SELECT COUNT(m) FROM SwiftMessage m WHERE " +
           "m.receivedDate >= :startOfDay AND m.receivedDate <= :endOfDay")
    long countMessagesReceivedToday(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    /**
     * Count error messages
     */
    @Query("SELECT COUNT(m) FROM SwiftMessage m WHERE " +
           "m.status IN ('PARSE_ERROR', 'VALIDATION_ERROR', 'PROCESSING_ERROR', 'REJECTED')")
    long countErrorMessages();

    /**
     * Find unprocessed high priority messages
     */
    @Query("SELECT m FROM SwiftMessage m WHERE " +
           "m.status IN ('RECEIVED', 'PROCESSING') " +
           "AND m.priority <= :maxPriority " +
           "ORDER BY m.priority ASC, m.receivedDate ASC")
    List<SwiftMessage> findHighPriorityUnprocessedMessages(@Param("maxPriority") Integer maxPriority);

    /**
     * Find messages without response (MT760 messages that haven't generated MT768/769)
     */
    @Query("SELECT m FROM SwiftMessage m WHERE " +
           "m.messageType = 'MT760' " +
           "AND m.status = 'PROCESSED' " +
           "AND m.responseMessage IS NULL " +
           "ORDER BY m.processingCompletedDate DESC")
    List<SwiftMessage> findProcessedMessagesWithoutResponse();

    /**
     * Find duplicate message references (for duplicate detection)
     */
    @Query("SELECT COUNT(m) FROM SwiftMessage m WHERE m.messageReference = :messageReference")
    long countByMessageReference(@Param("messageReference") String messageReference);

    /**
     * Statistics query: Message processing performance
     */
    @Query("SELECT m.status, COUNT(m), AVG(FUNCTION('TIMESTAMPDIFF', MINUTE, m.receivedDate, m.processingCompletedDate)) " +
           "FROM SwiftMessage m WHERE m.receivedDate >= :startDate " +
           "GROUP BY m.status")
    List<Object[]> getMessageProcessingStatistics(@Param("startDate") LocalDateTime startDate);

    /**
     * Find messages for audit report
     */
    @Query("SELECT m FROM SwiftMessage m WHERE " +
           "m.receivedDate >= :startDate AND m.receivedDate <= :endDate " +
           "AND (:messageType IS NULL OR m.messageType = :messageType) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "ORDER BY m.receivedDate DESC")
    Page<SwiftMessage> findMessagesForAudit(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("messageType") SwiftMessageType messageType,
            @Param("status") SwiftMessageStatus status,
            Pageable pageable
    );
}


