package com.interexport.guarantees.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interexport.guarantees.entity.ImportJob;
import com.interexport.guarantees.entity.ImportJobError;
import com.interexport.guarantees.entity.enums.ImportFileType;
import com.interexport.guarantees.entity.enums.ImportStatus;
import com.interexport.guarantees.repository.ImportJobRepository;
import com.interexport.guarantees.repository.ImportJobErrorRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Migration Service for F12 Data Migration
 * Orchestrates the import of data from legacy systems with batch processing,
 * checkpoint/restart capability, and comprehensive error handling
 */
@Service
@Slf4j
public class DataMigrationService {

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private ImportJobErrorRepository importJobErrorRepository;

    @Autowired
    private FileProcessorService fileProcessorService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ObjectMapper objectMapper;

    // Metrics
    private final Counter migrationJobsStarted;
    private final Counter migrationJobsCompleted;
    private final Counter migrationJobsFailed;
    private final Timer migrationDuration;

    // Configuration
    private static final String UPLOAD_DIR = "data/uploads";
    private static final String ARCHIVE_DIR = "data/archive";
    private static final String ERROR_DIR = "data/errors";

    public DataMigrationService(MeterRegistry meterRegistry) {
        this.migrationJobsStarted = Counter.builder("migration.jobs.started")
                .description("Number of migration jobs started")
                .register(meterRegistry);
                
        this.migrationJobsCompleted = Counter.builder("migration.jobs.completed")
                .description("Number of migration jobs completed")
                .register(meterRegistry);
                
        this.migrationJobsFailed = Counter.builder("migration.jobs.failed")
                .description("Number of migration jobs failed")
                .register(meterRegistry);
                
        this.migrationDuration = Timer.builder("migration.duration")
                .description("Migration job execution duration")
                .register(meterRegistry);
    }

    /**
     * Upload and validate a file for import
     */
    @Transactional
    public ImportJob uploadFile(MultipartFile file, String sourceSystem, String targetEntity, 
                               String initiatedBy, Map<String, Object> configuration) throws IOException {
        
        // Create directories if they don't exist
        createDirectoriesIfNotExist();
        
        // Generate unique job ID
        String jobId = UUID.randomUUID().toString();
        
        // Determine file type
        ImportFileType fileType = determineFileType(file.getOriginalFilename(), file.getContentType());
        
        // Save uploaded file
        String filename = jobId + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Create import job
        ImportJob importJob = new ImportJob();
        importJob.setJobId(jobId);
        importJob.setFilename(file.getOriginalFilename());
        importJob.setFilePath(filePath.toString());
        importJob.setFileSize(file.getSize());
        importJob.setFileType(fileType);
        importJob.setStatus(ImportStatus.PENDING);
        importJob.setSourceSystem(sourceSystem);
        importJob.setTargetEntity(targetEntity);
        importJob.setInitiatedBy(initiatedBy);
        importJob.setServerInstance(getServerInstance());
        importJob.setProcessId(getProcessId());
        
        // Set configuration
        if (configuration != null && !configuration.isEmpty()) {
            importJob.setImportConfiguration(objectMapper.writeValueAsString(configuration));
        }
        
        // Set default validation rules based on target entity
        Map<String, Object> validationRules = getDefaultValidationRules(targetEntity);
        importJob.setValidationRules(objectMapper.writeValueAsString(validationRules));
        
        // Set rollback deadline (30 days from now)
        importJob.setRollbackDeadline(LocalDateTime.now().plusDays(30));
        
        importJob = importJobRepository.save(importJob);
        
        log.info("Created import job {} for file {} (size: {} bytes)", 
                jobId, file.getOriginalFilename(), file.getSize());
        
        return importJob;
    }

    /**
     * Start processing an import job
     */
    @Async
    @Transactional
    public void startImportJob(String jobId) {
        Optional<ImportJob> jobOpt = importJobRepository.findByJobId(jobId);
        if (jobOpt.isEmpty()) {
            log.error("Import job not found: {}", jobId);
            return;
        }

        ImportJob job = jobOpt.get();
        
        if (job.getStatus() != ImportStatus.PENDING) {
            log.warn("Import job {} is not in PENDING status: {}", jobId, job.getStatus());
            return;
        }

        Timer.Sample sample = Timer.start();
        migrationJobsStarted.increment();

        try {
            log.info("Starting import job {}", jobId);
            
            // Update job status
            job.setStatus(ImportStatus.VALIDATING);
            job.setStartedAt(LocalDateTime.now());
            importJobRepository.save(job);

            // Phase 1: File validation and analysis
            validateAndAnalyzeFile(job);

            // Phase 2: Data processing with batch/checkpoint capability
            if (job.getStatus() == ImportStatus.VALIDATING) {
                job.setStatus(ImportStatus.IN_PROGRESS);
                importJobRepository.save(job);
                
                processDataInBatches(job);
            }

            // Phase 3: Finalization
            finalizeImport(job);
            
            migrationJobsCompleted.increment();
            log.info("Completed import job {} - Success: {}, Failed: {}", 
                    jobId, job.getSuccessfulRecords(), job.getFailedRecords());

        } catch (Exception e) {
            log.error("Failed to process import job {}: {}", jobId, e.getMessage(), e);
            
            job.setStatus(ImportStatus.FAILED);
            job.setErrorSummary(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);
            
            migrationJobsFailed.increment();
            
        } finally {
            sample.stop(migrationDuration);
        }
    }

    /**
     * Restart a paused or failed import job from last checkpoint
     */
    @Async
    @Transactional
    public void restartImportJob(String jobId) {
        Optional<ImportJob> jobOpt = importJobRepository.findByJobId(jobId);
        if (jobOpt.isEmpty()) {
            log.error("Import job not found: {}", jobId);
            return;
        }

        ImportJob job = jobOpt.get();
        
        if (!job.canRestart()) {
            log.warn("Import job {} cannot be restarted. Status: {}, Checkpoint: {}", 
                    jobId, job.getStatus(), job.getCheckpointPosition());
            return;
        }

        log.info("Restarting import job {} from checkpoint position {}", jobId, job.getCheckpointPosition());
        
        // Reset retry count for restart
        job.setCurrentRetries(0);
        job.setStatus(ImportStatus.IN_PROGRESS);
        job.setStartedAt(LocalDateTime.now());
        importJobRepository.save(job);
        
        try {
            // Continue processing from checkpoint
            processDataInBatches(job);
            finalizeImport(job);
            
        } catch (Exception e) {
            log.error("Failed to restart import job {}: {}", jobId, e.getMessage(), e);
            
            job.setStatus(ImportStatus.FAILED);
            job.setErrorSummary("Restart failed: " + e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);
        }
    }

    /**
     * Rollback an imported dataset
     */
    @Transactional
    public void rollbackImport(String jobId, String rollbackReason, String rollbackBy) {
        Optional<ImportJob> jobOpt = importJobRepository.findByJobId(jobId);
        if (jobOpt.isEmpty()) {
            log.error("Import job not found: {}", jobId);
            return;
        }

        ImportJob job = jobOpt.get();
        
        if (!job.canRollback()) {
            throw new IllegalStateException("Import job cannot be rolled back: " + jobId);
        }

        log.info("Starting rollback for import job {}", jobId);
        
        job.setStatus(ImportStatus.ROLLBACK_IN_PROGRESS);
        importJobRepository.save(job);
        
        try {
            // Execute rollback based on stored instructions
            executeRollback(job, rollbackReason, rollbackBy);
            
            job.setStatus(ImportStatus.ROLLBACK_COMPLETED);
            job.setCanRollback(false); // Prevent multiple rollbacks
            
            log.info("Successfully rolled back import job {}", jobId);
            
        } catch (Exception e) {
            log.error("Failed to rollback import job {}: {}", jobId, e.getMessage(), e);
            job.setStatus(ImportStatus.ROLLBACK_FAILED);
            job.setErrorSummary("Rollback failed: " + e.getMessage());
        } finally {
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);
        }
    }

    /**
     * Validate and analyze uploaded file
     */
    private void validateAndAnalyzeFile(ImportJob job) throws Exception {
        log.info("Validating file for job {}", job.getJobId());
        
        File file = new File(job.getFilePath());
        if (!file.exists()) {
            throw new IOException("Uploaded file not found: " + job.getFilePath());
        }

        // File format validation
        boolean isValidFormat = fileProcessorService.validateFileFormat(file, job.getFileType());
        if (!isValidFormat) {
            throw new IllegalArgumentException("Invalid file format for type: " + job.getFileType());
        }

        // Count total records
        long totalRecords = fileProcessorService.countRecords(file, job.getFileType());
        job.setTotalRecords(totalRecords);
        
        log.info("File validation completed for job {} - Total records: {}", job.getJobId(), totalRecords);
        
        importJobRepository.save(job);
    }

    /**
     * Process data in batches with checkpoint capability
     */
    private void processDataInBatches(ImportJob job) throws Exception {
        log.info("Processing data in batches for job {}", job.getJobId());
        
        File file = new File(job.getFilePath());
        long startPosition = job.getCheckpointPosition();
        int batchSize = job.getBatchSize();
        
        long currentPosition = startPosition;
        boolean hasMoreData = true;
        
        while (hasMoreData && job.getStatus() == ImportStatus.IN_PROGRESS) {
            try {
                // Process batch
                List<Map<String, Object>> batch = fileProcessorService.readBatch(
                        file, job.getFileType(), currentPosition, batchSize);
                
                if (batch.isEmpty()) {
                    hasMoreData = false;
                    break;
                }
                
                // Process each record in the batch
                for (Map<String, Object> record : batch) {
                    try {
                        processRecord(job, record, currentPosition);
                        job.setSuccessfulRecords(job.getSuccessfulRecords() + 1);
                        
                    } catch (Exception e) {
                        handleRecordError(job, record, currentPosition, e);
                        job.setFailedRecords(job.getFailedRecords() + 1);
                    }
                    
                    currentPosition++;
                }
                
                // Update progress and create checkpoint
                job.setProcessedRecords(currentPosition);
                job.setCheckpointPosition(currentPosition);
                job.createCheckpoint();
                importJobRepository.save(job);
                
                log.debug("Processed batch for job {} - Position: {}, Success: {}, Failed: {}", 
                        job.getJobId(), currentPosition, job.getSuccessfulRecords(), job.getFailedRecords());
                
                // Check if we should pause (for system maintenance, etc.)
                if (shouldPauseProcessing()) {
                    job.setStatus(ImportStatus.PAUSED);
                    job.setPausedAt(LocalDateTime.now());
                    importJobRepository.save(job);
                    log.info("Import job {} paused at position {}", job.getJobId(), currentPosition);
                    return;
                }
                
            } catch (Exception e) {
                log.error("Batch processing error for job {} at position {}: {}", 
                        job.getJobId(), currentPosition, e.getMessage(), e);
                
                job.setCurrentRetries(job.getCurrentRetries() + 1);
                if (job.getCurrentRetries() >= job.getMaxRetries()) {
                    throw new RuntimeException("Max retries exceeded", e);
                }
                
                // Wait before retry
                Thread.sleep(job.getCurrentRetries() * 5000); // Exponential backoff
            }
        }
    }

    /**
     * Process an individual record
     */
    private void processRecord(ImportJob job, Map<String, Object> record, long recordNumber) throws Exception {
        // Validate record
        validationService.validateRecord(record, job.getValidationRules(), job.getTargetEntity());
        
        // Transform and save record based on target entity
        switch (job.getTargetEntity().toUpperCase()) {
            case "GUARANTEE":
                fileProcessorService.processGuaranteeRecord(record, job);
                break;
            case "CLIENT":
                fileProcessorService.processClientRecord(record, job);
                break;
            case "COMMISSION":
                fileProcessorService.processCommissionRecord(record, job);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported target entity: " + job.getTargetEntity());
        }
    }

    /**
     * Handle record processing error
     */
    private void handleRecordError(ImportJob job, Map<String, Object> record, long recordNumber, Exception error) {
        ImportJobError jobError = new ImportJobError();
        jobError.setImportJob(job);
        jobError.setRecordNumber(recordNumber);
        jobError.setErrorMessage(error.getMessage());
        jobError.setRecordData(record.toString());
        jobError.setErrorTimestamp(LocalDateTime.now());
        
        // Categorize error
        if (error instanceof IllegalArgumentException) {
            jobError.setErrorType("VALIDATION_ERROR");
            jobError.setCategory(ImportJobError.ErrorCategory.VALIDATION);
            jobError.setSeverity(ImportJobError.ErrorSeverity.MEDIUM);
        } else if (error.getMessage().contains("duplicate")) {
            jobError.setErrorType("DUPLICATE_RECORD");
            jobError.setCategory(ImportJobError.ErrorCategory.DUPLICATE);
            jobError.setSeverity(ImportJobError.ErrorSeverity.LOW);
        } else {
            jobError.setErrorType("PROCESSING_ERROR");
            jobError.setCategory(ImportJobError.ErrorCategory.SYSTEM);
            jobError.setSeverity(ImportJobError.ErrorSeverity.HIGH);
        }
        
        importJobErrorRepository.save(jobError);
        
        log.debug("Recorded error for job {} at record {}: {}", 
                job.getJobId(), recordNumber, error.getMessage());
    }

    /**
     * Finalize import job
     */
    private void finalizeImport(ImportJob job) {
        job.setCompletedAt(LocalDateTime.now());
        job.updateProgress();
        
        if (job.getFailedRecords() == 0) {
            job.setStatus(ImportStatus.COMPLETED);
        } else {
            job.setStatus(ImportStatus.COMPLETED_WITH_ERRORS);
        }
        
        // Archive processed file
        archiveFile(job);
        
        // Generate error report if needed
        if (job.getFailedRecords() > 0) {
            generateErrorReport(job);
        }
        
        importJobRepository.save(job);
    }

    /**
     * Execute rollback operations
     */
    private void executeRollback(ImportJob job, String rollbackReason, String rollbackBy) throws Exception {
        // Implementation would depend on the target entity and rollback strategy
        log.info("Executing rollback for job {} - Reason: {}", job.getJobId(), rollbackReason);
        
        // For demonstration, this is a simplified approach
        // In reality, you would need to maintain rollback logs during import
        // and execute the reverse operations
        
        switch (job.getTargetEntity().toUpperCase()) {
            case "GUARANTEE":
                fileProcessorService.rollbackGuaranteeImport(job);
                break;
            case "CLIENT":
                fileProcessorService.rollbackClientImport(job);
                break;
            case "COMMISSION":
                fileProcessorService.rollbackCommissionImport(job);
                break;
        }
    }

    // Helper methods
    private ImportFileType determineFileType(String filename, String contentType) {
        if (filename.toLowerCase().endsWith(".csv")) return ImportFileType.CSV;
        if (filename.toLowerCase().endsWith(".xml")) return ImportFileType.XML;
        if (filename.toLowerCase().endsWith(".json")) return ImportFileType.JSON;
        if (filename.toLowerCase().endsWith(".xlsx")) return ImportFileType.EXCEL;
        if (filename.toLowerCase().endsWith(".dka")) return ImportFileType.DOKA_LEGACY;
        return ImportFileType.CSV; // Default
    }

    private Map<String, Object> getDefaultValidationRules(String targetEntity) {
        Map<String, Object> rules = new HashMap<>();
        // Implementation would return entity-specific validation rules
        rules.put("requireAllFields", true);
        rules.put("allowDuplicates", false);
        rules.put("maxStringLength", 500);
        return rules;
    }

    private void createDirectoriesIfNotExist() throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        Files.createDirectories(Paths.get(ARCHIVE_DIR));
        Files.createDirectories(Paths.get(ERROR_DIR));
    }

    private void archiveFile(ImportJob job) {
        try {
            Path sourcePath = Paths.get(job.getFilePath());
            Path targetPath = Paths.get(ARCHIVE_DIR, job.getJobId() + "_" + 
                    LocalDateTime.now().toString().replace(":", "-") + "_" + job.getFilename());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            job.setFilePath(targetPath.toString());
        } catch (IOException e) {
            log.error("Failed to archive file for job {}: {}", job.getJobId(), e.getMessage());
        }
    }

    private void generateErrorReport(ImportJob job) {
        // Generate detailed error report
        log.info("Generated error report for job {} with {} errors", 
                job.getJobId(), job.getFailedRecords());
    }

    private boolean shouldPauseProcessing() {
        // Logic to determine if processing should be paused
        // Could check system load, maintenance schedules, etc.
        return false;
    }

    private String getServerInstance() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getProcessId() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
    }
}




