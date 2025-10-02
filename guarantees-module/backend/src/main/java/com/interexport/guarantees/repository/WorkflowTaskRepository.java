package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.WorkflowTask;
import com.interexport.guarantees.entity.enums.WorkflowStep;
import com.interexport.guarantees.entity.enums.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowTask entities
 * Supports F11 - Kafka Workflow Engine
 */
@Repository
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {

    /**
     * Find workflow task by task ID
     */
    Optional<WorkflowTask> findByTaskId(String taskId);

    /**
     * Find all tasks for a workflow execution
     */
    List<WorkflowTask> findByWorkflowExecutionIdOrderByStepOrder(Long workflowExecutionId);

    /**
     * Find tasks by execution ID and status
     */
    List<WorkflowTask> findByWorkflowExecutionIdAndStatus(Long workflowExecutionId, WorkflowStatus status);

    /**
     * Find tasks by step and status
     */
    List<WorkflowTask> findByStepAndStatus(WorkflowStep step, WorkflowStatus status);

    /**
     * Find tasks ready for execution (scheduled and due)
     */
    @Query("SELECT wt FROM WorkflowTask wt WHERE wt.status = 'PENDING' AND " +
           "(wt.scheduledAt IS NULL OR wt.scheduledAt <= :now) " +
           "ORDER BY wt.scheduledAt ASC, wt.stepOrder ASC")
    List<WorkflowTask> findTasksReadyForExecution(@Param("now") LocalDateTime now);

    /**
     * Find tasks ready for retry (due for retry and retry count not exceeded)
     */
    @Query("SELECT wt FROM WorkflowTask wt WHERE wt.status = 'RETRYING' AND " +
           "wt.retryCount < wt.maxRetries AND " +
           "(wt.nextRetryAt IS NULL OR wt.nextRetryAt <= :now) " +
           "ORDER BY wt.nextRetryAt ASC")
    List<WorkflowTask> findTasksReadyForRetry(@Param("now") LocalDateTime now);

    /**
     * Find tasks in Dead Letter Queue for manual intervention
     */
    List<WorkflowTask> findByStatus(WorkflowStatus status);

    /**
     * Find tasks by trace ID for OpenTelemetry correlation
     */
    List<WorkflowTask> findByTraceId(String traceId);

    /**
     * Find tasks that have been in progress too long (potential timeouts)
     */
    @Query("SELECT wt FROM WorkflowTask wt WHERE wt.status = 'IN_PROGRESS' AND wt.startedAt <= :timeoutThreshold")
    List<WorkflowTask> findTimedOutTasks(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * Find next pending task for a workflow execution
     */
    @Query("SELECT wt FROM WorkflowTask wt WHERE wt.workflowExecution.id = :executionId AND " +
           "wt.status = 'PENDING' ORDER BY wt.stepOrder ASC")
    Optional<WorkflowTask> findNextPendingTask(@Param("executionId") Long executionId);

    /**
     * Count tasks by status for monitoring
     */
    long countByStatus(WorkflowStatus status);

    /**
     * Count tasks by step and status
     */
    long countByStepAndStatus(WorkflowStep step, WorkflowStatus status);

    /**
     * Count failed tasks by workflow execution
     */
    long countByWorkflowExecutionIdAndStatus(Long workflowExecutionId, WorkflowStatus status);

    /**
     * Find tasks with high retry counts for alerting
     */
    @Query("SELECT wt FROM WorkflowTask wt WHERE wt.retryCount >= :threshold AND wt.status != 'COMPLETED'")
    List<WorkflowTask> findTasksWithHighRetryCount(@Param("threshold") int threshold);

    /**
     * Find tasks by Kafka topic for debugging
     */
    List<WorkflowTask> findByKafkaTopic(String kafkaTopic);
}




