package com.interexport.guarantees.entity;

import com.interexport.guarantees.entity.enums.ImportFileType;
import com.interexport.guarantees.entity.enums.ImportStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ImportJob entity for F12 Data Migration
 * Tracks data import jobs with checkpoint/restart capability
 */
@Entity
@Table(name = "import_jobs")
@Data
@EqualsAndHashCode(callSuper = true)
public class ImportJob extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String jobId; // UUID for tracking

    @Column(nullable = false, length = 200)
    private String filename;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportFileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @Column(length = 100)
    private String sourceSystem; // e.g., "DOKA_LEGACY", "OLD_GUARANTEE_SYSTEM"

    @Column(length = 100)
    private String targetEntity; // e.g., "GUARANTEE", "CLIENT", "COMMISSION"

    @Column(columnDefinition = "TEXT")
    private String importConfiguration; // JSON with import-specific settings

    @Column(columnDefinition = "TEXT")
    private String validationRules; // JSON with validation criteria

    // Progress tracking
    private Long totalRecords = 0L;
    private Long processedRecords = 0L;
    private Long successfulRecords = 0L;
    private Long failedRecords = 0L;
    private Long skippedRecords = 0L;

    // Checkpoint/Restart capability
    private Long checkpointPosition = 0L;
    private Integer batchSize = 1000;
    private Integer maxRetries = 3;
    private Integer currentRetries = 0;

    // Timing
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime lastCheckpointAt;

    // Error handling and rollback
    @Column(columnDefinition = "TEXT")
    private String errorSummary;

    @Column(columnDefinition = "TEXT")
    private String rollbackInstructions; // JSON with rollback steps

    private Boolean canRollback = true;
    private LocalDateTime rollbackDeadline; // After this date, rollback not allowed

    // Performance metrics
    private Long executionTimeMs = 0L;
    private Double recordsPerSecond = 0.0;
    private Long peakMemoryUsageMb = 0L;

    // User and system info
    @Column(length = 100)
    private String initiatedBy; // User or system that started the import

    @Column(length = 100)
    private String serverInstance; // Which server instance is processing

    @Column(length = 100)
    private String processId; // Process ID for monitoring

    // Bidirectional relationship with ImportJobError
    @OneToMany(mappedBy = "importJob", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImportJobError> errors = new ArrayList<>();

    // Helper methods
    public void addError(ImportJobError error) {
        errors.add(error);
        error.setImportJob(this);
        this.failedRecords++;
    }

    public void removeError(ImportJobError error) {
        errors.remove(error);
        error.setImportJob(null);
    }

    public boolean isCompleted() {
        return status == ImportStatus.COMPLETED || 
               status == ImportStatus.COMPLETED_WITH_ERRORS;
    }

    public boolean isFailed() {
        return status == ImportStatus.FAILED;
    }

    public boolean canRestart() {
        return status == ImportStatus.PAUSED || 
               status == ImportStatus.FAILED ||
               (status == ImportStatus.IN_PROGRESS && checkpointPosition > 0);
    }

    public boolean canRollback() {
        return canRollback && 
               isCompleted() && 
               (rollbackDeadline == null || LocalDateTime.now().isBefore(rollbackDeadline));
    }

    public double getProgressPercentage() {
        if (totalRecords == 0) return 0.0;
        return (double) processedRecords / totalRecords * 100.0;
    }

    public void updateProgress() {
        if (startedAt != null && processedRecords > 0) {
            long elapsedMs = java.time.Duration.between(startedAt, LocalDateTime.now()).toMillis();
            if (elapsedMs > 0) {
                this.recordsPerSecond = (double) processedRecords / (elapsedMs / 1000.0);
            }
        }
    }

    public void createCheckpoint() {
        this.lastCheckpointAt = LocalDateTime.now();
        updateProgress();
    }
}




