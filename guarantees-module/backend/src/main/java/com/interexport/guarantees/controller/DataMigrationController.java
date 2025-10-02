package com.interexport.guarantees.controller;

import com.interexport.guarantees.entity.ImportJob;
import com.interexport.guarantees.entity.ImportJobError;
import com.interexport.guarantees.entity.enums.ImportStatus;
import com.interexport.guarantees.repository.ImportJobRepository;
import com.interexport.guarantees.repository.ImportJobErrorRepository;
import com.interexport.guarantees.service.DataMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for F12 Data Migration
 * Provides endpoints for uploading files, managing import jobs, and monitoring migration progress
 */
@RestController
@RequestMapping("/api/v1/data-migration")
@Tag(name = "Data Migration", description = "F12 Legacy Doka system import utilities")
public class DataMigrationController {

    @Autowired
    private DataMigrationService dataMigrationService;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private ImportJobErrorRepository importJobErrorRepository;

    /**
     * Upload a file for data import
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload Import File", description = "Upload a file for data import from legacy systems")
    public ResponseEntity<ImportJob> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Source system name") @RequestParam String sourceSystem,
            @Parameter(description = "Target entity type") @RequestParam String targetEntity,
            @Parameter(description = "User who initiated the import") @RequestParam String initiatedBy,
            @Parameter(description = "Import configuration") @RequestParam(required = false) Map<String, Object> configuration) {
        
        try {
            if (configuration == null) {
                configuration = new HashMap<>();
            }
            
            ImportJob importJob = dataMigrationService.uploadFile(file, sourceSystem, targetEntity, initiatedBy, configuration);
            return ResponseEntity.ok(importJob);
            
        } catch (Exception e) {
            // In a production system, would use proper exception handling
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Start processing an import job
     */
    @PostMapping("/jobs/{jobId}/start")
    @Operation(summary = "Start Import Job", description = "Start processing an uploaded import job")
    public ResponseEntity<Map<String, String>> startImportJob(
            @Parameter(description = "Import Job ID") @PathVariable String jobId) {
        
        Optional<ImportJob> jobOpt = importJobRepository.findByJobId(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ImportJob job = jobOpt.get();
        if (job.getStatus() != ImportStatus.PENDING) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Job is not in PENDING status: " + job.getStatus());
            return ResponseEntity.badRequest().body(response);
        }

        // Start job asynchronously
        dataMigrationService.startImportJob(jobId);

        Map<String, String> response = new HashMap<>();
        response.put("status", "STARTED");
        response.put("message", "Import job has been started");
        return ResponseEntity.ok(response);
    }

    /**
     * Restart a paused or failed import job
     */
    @PostMapping("/jobs/{jobId}/restart")
    @Operation(summary = "Restart Import Job", description = "Restart a paused or failed import job from last checkpoint")
    public ResponseEntity<Map<String, String>> restartImportJob(
            @Parameter(description = "Import Job ID") @PathVariable String jobId) {
        
        Optional<ImportJob> jobOpt = importJobRepository.findByJobId(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ImportJob job = jobOpt.get();
        if (!job.canRestart()) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Job cannot be restarted. Status: " + job.getStatus());
            return ResponseEntity.badRequest().body(response);
        }

        // Restart job asynchronously
        dataMigrationService.restartImportJob(jobId);

        Map<String, String> response = new HashMap<>();
        response.put("status", "RESTARTED");
        response.put("message", "Import job has been restarted from checkpoint");
        return ResponseEntity.ok(response);
    }

    /**
     * Get import job details
     */
    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get Import Job", description = "Retrieve import job details and progress")
    public ResponseEntity<ImportJob> getImportJob(
            @Parameter(description = "Import Job ID") @PathVariable String jobId) {
        
        Optional<ImportJob> job = importJobRepository.findByJobId(jobId);
        return job.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all import jobs
     */
    @GetMapping("/jobs")
    @Operation(summary = "List Import Jobs", description = "Retrieve all import jobs with optional filtering")
    public ResponseEntity<List<ImportJob>> getImportJobs(
            @Parameter(description = "Filter by status") @RequestParam(required = false) ImportStatus status,
            @Parameter(description = "Filter by source system") @RequestParam(required = false) String sourceSystem,
            @Parameter(description = "Filter by target entity") @RequestParam(required = false) String targetEntity) {
        
        List<ImportJob> jobs;
        
        if (status != null && sourceSystem != null) {
            jobs = importJobRepository.findByStatusAndSourceSystem(status, sourceSystem);
        } else if (status != null) {
            jobs = importJobRepository.findByStatus(status);
        } else if (targetEntity != null) {
            jobs = importJobRepository.findByTargetEntityOrderByCreatedDateDesc(targetEntity);
        } else {
            jobs = importJobRepository.findAll();
        }
        
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get errors for an import job
     */
    @GetMapping("/jobs/{jobId}/errors")
    @Operation(summary = "Get Job Errors", description = "Retrieve errors for a specific import job")
    public ResponseEntity<List<ImportJobError>> getJobErrors(
            @Parameter(description = "Import Job ID") @PathVariable String jobId) {
        
        Optional<ImportJob> jobOpt = importJobRepository.findByJobId(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<ImportJobError> errors = importJobErrorRepository.findByImportJobIdOrderByRecordNumber(jobOpt.get().getId());
        return ResponseEntity.ok(errors);
    }

    /**
     * Get import job statistics
     */
    @GetMapping("/jobs/{jobId}/stats")
    @Operation(summary = "Get Job Statistics", description = "Retrieve detailed statistics for an import job")
    public ResponseEntity<Map<String, Object>> getJobStatistics(
            @Parameter(description = "Import Job ID") @PathVariable String jobId) {
        
        Optional<ImportJob> jobOpt = importJobRepository.findByJobId(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ImportJob job = jobOpt.get();
        Map<String, Object> stats = new HashMap<>();
        
        // Basic statistics
        stats.put("totalRecords", job.getTotalRecords());
        stats.put("processedRecords", job.getProcessedRecords());
        stats.put("successfulRecords", job.getSuccessfulRecords());
        stats.put("failedRecords", job.getFailedRecords());
        stats.put("skippedRecords", job.getSkippedRecords());
        stats.put("progressPercentage", job.getProgressPercentage());
        stats.put("recordsPerSecond", job.getRecordsPerSecond());
        
        // Error statistics
        Object[] errorStats = importJobErrorRepository.getErrorStatisticsForJob(job.getId());
        if (errorStats != null && errorStats.length >= 3) {
            stats.put("totalErrors", errorStats[0]);
            stats.put("resolvedErrors", errorStats[1]);
            stats.put("criticalErrors", errorStats[2]);
        }
        
        // Error breakdown by category
        List<Object[]> errorsByCategory = importJobErrorRepository.countErrorsByCategoryForJob(job.getId());
        Map<String, Long> categoryBreakdown = new HashMap<>();
        for (Object[] row : errorsByCategory) {
            categoryBreakdown.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("errorsByCategory", categoryBreakdown);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Rollback an import job
     */
    @PostMapping("/jobs/{jobId}/rollback")
    @Operation(summary = "Rollback Import", description = "Rollback a completed import job")
    public ResponseEntity<Map<String, String>> rollbackImport(
            @Parameter(description = "Import Job ID") @PathVariable String jobId,
            @Parameter(description = "Rollback reason") @RequestParam String rollbackReason,
            @Parameter(description = "User performing rollback") @RequestParam String rollbackBy) {
        
        try {
            dataMigrationService.rollbackImport(jobId, rollbackReason, rollbackBy);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "ROLLBACK_STARTED");
            response.put("message", "Import rollback has been initiated");
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get overall data migration statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get Migration Statistics", description = "Retrieve overall data migration statistics")
    public ResponseEntity<Map<String, Object>> getMigrationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Job statistics by status
        Map<String, Long> jobsByStatus = new HashMap<>();
        for (ImportStatus status : ImportStatus.values()) {
            jobsByStatus.put(status.name(), importJobRepository.countByStatus(status));
        }
        stats.put("jobsByStatus", jobsByStatus);
        
        // Statistics by source system
        List<Object[]> sourceSystemStats = importJobRepository.getImportStatsBySourceSystem();
        Map<String, Object> sourceSystemBreakdown = new HashMap<>();
        for (Object[] row : sourceSystemStats) {
            String sourceSystem = (String) row[0];
            Long jobCount = (Long) row[1];
            Long successfulRecords = row[2] != null ? (Long) row[2] : 0L;
            Long failedRecords = row[3] != null ? (Long) row[3] : 0L;
            
            Map<String, Object> systemStats = new HashMap<>();
            systemStats.put("jobCount", jobCount);
            systemStats.put("successfulRecords", successfulRecords);
            systemStats.put("failedRecords", failedRecords);
            
            sourceSystemBreakdown.put(sourceSystem, systemStats);
        }
        stats.put("sourceSystemStats", sourceSystemBreakdown);
        
        // Recent activity
        List<ImportJob> activeJobs = importJobRepository.findActiveJobs();
        stats.put("activeJobs", activeJobs.size());
        
        List<ImportJob> rollbackableJobs = importJobRepository.findRollbackableJobs(java.time.LocalDateTime.now());
        stats.put("rollbackableJobs", rollbackableJobs.size());
        
        // DLQ-like statistics for data migration
        List<ImportJob> highErrorJobs = importJobRepository.findJobsWithHighErrorRate(0.1); // 10% error rate
        stats.put("highErrorRateJobs", highErrorJobs.size());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get system health check for data migration
     */
    @GetMapping("/health")
    @Operation(summary = "Migration Health Check", description = "Check the health of the data migration system")
    public ResponseEntity<Map<String, Object>> getMigrationHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check active jobs
            long activeJobs = importJobRepository.findActiveJobs().size();
            health.put("activeJobs", activeJobs);
            
            // Check jobs that might be stuck
            java.time.LocalDateTime timeoutThreshold = java.time.LocalDateTime.now().minusHours(2);
            long timedOutJobs = importJobRepository.findTimedOutJobs(timeoutThreshold).size();
            health.put("timedOutJobs", timedOutJobs);
            
            // Check jobs that can be restarted
            long restartableJobs = importJobRepository.findRestartableJobs().size();
            health.put("restartableJobs", restartableJobs);
            
            // Check unresolved errors
            long unresolvedErrors = importJobErrorRepository.countByIsResolvedFalse();
            health.put("unresolvedErrors", unresolvedErrors);
            
            // Determine overall health
            String status = "HEALTHY";
            if (timedOutJobs > 0 || unresolvedErrors > 100) {
                status = "WARNING";
            }
            if (timedOutJobs > 5 || unresolvedErrors > 500) {
                status = "CRITICAL";
            }
            
            health.put("status", status);
            health.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "ERROR");
            health.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(health);
        }
    }
}




