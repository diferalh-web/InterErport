package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.WorkflowStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkflowExecution entity for F11 Kafka Workflow Engine
 * Tracks the overall execution of a workflow for a specific guarantee
 */
@Entity
@Table(name = "workflow_executions")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowExecution extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String executionId; // UUID for tracking across systems

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantee_id", nullable = false)
    private GuaranteeContract guarantee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Column(nullable = false, length = 100)
    private String workflowType; // e.g., "POST_APPROVAL", "AMENDMENT", "EXPIRY"

    @Column(length = 100)
    private String triggerEvent; // e.g., "GUARANTEE_APPROVED", "AMENDMENT_APPROVED"

    @Column(columnDefinition = "TEXT")
    private String workflowDefinition; // JSON representation of the workflow steps

    @Column(columnDefinition = "TEXT")
    private String executionContext; // JSON with execution-specific data

    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastRetryAt;

    private Integer totalSteps;
    private Integer completedSteps;
    private Integer failedSteps;
    private Integer retryCount = 0;
    private Integer maxRetries = 3;

    @Column(length = 100)
    private String traceId; // OpenTelemetry trace ID

    @Column(length = 100)
    private String spanId; // OpenTelemetry span ID

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String executionLog; // Detailed execution log

    // Bidirectional relationship with WorkflowTask
    @OneToMany(mappedBy = "workflowExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowTask> tasks = new ArrayList<>();

    // Helper methods
    public void addTask(WorkflowTask task) {
        tasks.add(task);
        task.setWorkflowExecution(this);
    }

    public void removeTask(WorkflowTask task) {
        tasks.remove(task);
        task.setWorkflowExecution(null);
    }

    public boolean isCompleted() {
        return status == WorkflowStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == WorkflowStatus.FAILED || status == WorkflowStatus.DLQ;
    }

    public boolean canRetry() {
        return retryCount < maxRetries && !isFailed();
    }
}




