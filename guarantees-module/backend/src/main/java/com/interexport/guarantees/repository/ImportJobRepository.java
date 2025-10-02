package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.ImportJob;
import com.interexport.guarantees.entity.enums.ImportFileType;
import com.interexport.guarantees.entity.enums.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ImportJob entities
 * Supports F12 - Data Migration with checkpoint/restart capability
 */
@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    /**
     * Find import job by job ID
     */
    Optional<ImportJob> findByJobId(String jobId);

    /**
     * Find jobs by status
     */
    List<ImportJob> findByStatus(ImportStatus status);

    /**
     * Find jobs by status and source system
     */
    List<ImportJob> findByStatusAndSourceSystem(ImportStatus status, String sourceSystem);

    /**
     * Find jobs by file type
     */
    List<ImportJob> findByFileType(ImportFileType fileType);

    /**
     * Find jobs that can be restarted (paused or failed with checkpoint)
     */
    @Query("SELECT ij FROM ImportJob ij WHERE (ij.status = 'PAUSED' OR ij.status = 'FAILED') AND ij.checkpointPosition > 0")
    List<ImportJob> findRestartableJobs();

    /**
     * Find jobs that can be rolled back
     */
    @Query("SELECT ij FROM ImportJob ij WHERE ij.canRollback = true AND ij.status IN ('COMPLETED', 'COMPLETED_WITH_ERRORS') " +
           "AND (ij.rollbackDeadline IS NULL OR ij.rollbackDeadline > :now)")
    List<ImportJob> findRollbackableJobs(@Param("now") LocalDateTime now);

    /**
     * Find jobs initiated by a specific user
     */
    List<ImportJob> findByInitiatedByOrderByCreatedDateDesc(String initiatedBy);

    /**
     * Find active jobs (in progress or validating)
     */
    @Query("SELECT ij FROM ImportJob ij WHERE ij.status IN ('IN_PROGRESS', 'VALIDATING')")
    List<ImportJob> findActiveJobs();

    /**
     * Find jobs that have been running too long (potential hung processes)
     */
    @Query("SELECT ij FROM ImportJob ij WHERE ij.status = 'IN_PROGRESS' AND ij.startedAt < :timeoutThreshold")
    List<ImportJob> findTimedOutJobs(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * Find recent completed jobs for monitoring
     */
    @Query("SELECT ij FROM ImportJob ij WHERE ij.status IN ('COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED') " +
           "AND ij.completedAt > :since ORDER BY ij.completedAt DESC")
    List<ImportJob> findRecentCompletedJobs(@Param("since") LocalDateTime since);

    /**
     * Get import statistics by status
     */
    @Query("SELECT ij.status, COUNT(ij) FROM ImportJob ij GROUP BY ij.status")
    List<Object[]> getImportStatsByStatus();

    /**
     * Get import statistics by source system
     */
    @Query("SELECT ij.sourceSystem, COUNT(ij), SUM(ij.successfulRecords), SUM(ij.failedRecords) " +
           "FROM ImportJob ij GROUP BY ij.sourceSystem")
    List<Object[]> getImportStatsBySourceSystem();

    /**
     * Count jobs by status
     */
    long countByStatus(ImportStatus status);

    /**
     * Count jobs by source system and status
     */
    long countBySourceSystemAndStatus(String sourceSystem, ImportStatus status);

    /**
     * Find jobs with high error rates for alerting
     */
    @Query("SELECT ij FROM ImportJob ij WHERE ij.failedRecords > 0 AND " +
           "(CAST(ij.failedRecords AS double) / CAST(ij.processedRecords AS double)) > :errorRateThreshold")
    List<ImportJob> findJobsWithHighErrorRate(@Param("errorRateThreshold") double errorRateThreshold);

    /**
     * Find jobs by target entity type
     */
    List<ImportJob> findByTargetEntityOrderByCreatedDateDesc(String targetEntity);

    /**
     * Find jobs created within date range
     */
    @Query("SELECT ij FROM ImportJob ij WHERE ij.createdDate BETWEEN :startDate AND :endDate ORDER BY ij.createdDate DESC")
    List<ImportJob> findJobsCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}




