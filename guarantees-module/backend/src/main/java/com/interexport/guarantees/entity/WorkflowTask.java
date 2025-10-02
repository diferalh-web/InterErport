package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.WorkflowStep;
import com.interexport.guarantees.entity.enums.WorkflowStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * WorkflowTask entity for F11 Kafka Workflow Engine
 * Tracks individual workflow steps within a workflow execution
 */
@Entity
@Table(name = "workflow_tasks")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_execution_id", nullable = false)
    private WorkflowExecution workflowExecution;

    @Column(nullable = false, length = 50)
    private String taskId; // UUID for individual task tracking

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStep step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Column(nullable = false)
    private Integer stepOrder; // Order of execution within the workflow

    @Column(length = 200)
    private String stepDescription;

    @Column(columnDefinition = "TEXT")
    private String inputParameters; // JSON with step-specific input data

    @Column(columnDefinition = "TEXT")
    private String outputResult; // JSON with step execution results

    @Column(columnDefinition = "TEXT")
    private String errorDetails; // Detailed error information

    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime nextRetryAt;

    private Integer retryCount = 0;
    private Integer maxRetries = 3;
    private Long retryDelaySeconds = 30L; // Initial retry delay

    @Column(length = 100)
    private String traceId; // OpenTelemetry trace ID

    @Column(length = 100)
    private String spanId; // OpenTelemetry span ID

    @Column(length = 100)
    private String parentSpanId; // Parent span for correlation

    // Kafka-specific fields
    @Column(length = 100)
    private String kafkaTopic;

    @Column(length = 50)
    private String kafkaPartition;

    @Column(length = 100)
    private String kafkaOffset;

    @Column(length = 100)
    private String messageKey;

    // Performance metrics
    private Long executionDurationMs;
    private Integer processingAttempts = 0;

    // Helper methods
    public boolean isCompleted() {
        return status == WorkflowStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == WorkflowStatus.FAILED || status == WorkflowStatus.DLQ;
    }

    public boolean canRetry() {
        return retryCount < maxRetries && 
               status != WorkflowStatus.COMPLETED && 
               status != WorkflowStatus.DLQ &&
               status != WorkflowStatus.CANCELLED;
    }

    public boolean isDue() {
        if (nextRetryAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(nextRetryAt);
    }

    public void incrementRetry() {
        this.retryCount++;
        this.processingAttempts++;
        
        // Exponential backoff: 30s, 60s, 120s, 240s, etc.
        long nextDelay = retryDelaySeconds * (long) Math.pow(2, Math.min(retryCount - 1, 6));
        this.nextRetryAt = LocalDateTime.now().plusSeconds(nextDelay);
        
        if (retryCount >= maxRetries) {
            this.status = WorkflowStatus.DLQ;
        } else {
            this.status = WorkflowStatus.RETRYING;
        }
    }
}




