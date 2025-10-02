package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.WorkflowExecution;
import com.interexport.guarantees.entity.enums.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowExecution entities
 * Supports F11 - Kafka Workflow Engine
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    /**
     * Find workflow execution by execution ID
     */
    Optional<WorkflowExecution> findByExecutionId(String executionId);

    /**
     * Find all workflow executions for a guarantee
     */
    List<WorkflowExecution> findByGuaranteeId(Long guaranteeId);

    /**
     * Find executions by status
     */
    List<WorkflowExecution> findByStatus(WorkflowStatus status);

    /**
     * Find executions by status and workflow type
     */
    List<WorkflowExecution> findByStatusAndWorkflowType(WorkflowStatus status, String workflowType);

    /**
     * Find executions scheduled before a certain time
     */
    @Query("SELECT we FROM WorkflowExecution we WHERE we.status = :status AND we.scheduledAt <= :scheduledBefore")
    List<WorkflowExecution> findScheduledExecutions(@Param("status") WorkflowStatus status, 
                                                   @Param("scheduledBefore") LocalDateTime scheduledBefore);

    /**
     * Find executions that can be retried
     */
    @Query("SELECT we FROM WorkflowExecution we WHERE we.status = :status AND we.retryCount < we.maxRetries AND we.lastRetryAt <= :retryBefore")
    List<WorkflowExecution> findRetryableExecutions(@Param("status") WorkflowStatus status, 
                                                   @Param("retryBefore") LocalDateTime retryBefore);

    /**
     * Find active executions (not completed, failed, or cancelled)
     */
    @Query("SELECT we FROM WorkflowExecution we WHERE we.status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED', 'DLQ')")
    List<WorkflowExecution> findActiveExecutions();

    /**
     * Find executions by trace ID for OpenTelemetry correlation
     */
    List<WorkflowExecution> findByTraceId(String traceId);

    /**
     * Find executions that have been in progress too long (potential timeouts)
     */
    @Query("SELECT we FROM WorkflowExecution we WHERE we.status = 'IN_PROGRESS' AND we.startedAt <= :timeoutThreshold")
    List<WorkflowExecution> findTimedOutExecutions(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * Count executions by status for monitoring
     */
    long countByStatus(WorkflowStatus status);

    /**
     * Count executions by workflow type and status
     */
    long countByWorkflowTypeAndStatus(String workflowType, WorkflowStatus status);
}




