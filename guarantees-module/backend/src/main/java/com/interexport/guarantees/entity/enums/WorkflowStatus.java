package com.interexport.guarantees.entity.enums;

/**
 * Status of workflow executions and tasks for F11 Kafka Workflow Engine
 */
public enum WorkflowStatus {
    PENDING("Pending", "Task is queued for execution"),
    IN_PROGRESS("In Progress", "Task is currently being executed"),
    COMPLETED("Completed", "Task has been successfully completed"),
    FAILED("Failed", "Task execution has failed"),
    RETRYING("Retrying", "Task is being retried after a failure"),
    SKIPPED("Skipped", "Task was skipped due to business rules"),
    CANCELLED("Cancelled", "Task was cancelled before completion"),
    TIMEOUT("Timeout", "Task execution timed out"),
    DLQ("Dead Letter Queue", "Task sent to Dead Letter Queue after max retries");

    private final String displayName;
    private final String description;

    WorkflowStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}




