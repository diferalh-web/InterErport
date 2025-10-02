package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.ImportJobError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ImportJobError entities
 * Supports F12 - Data Migration error tracking and analysis
 */
@Repository
public interface ImportJobErrorRepository extends JpaRepository<ImportJobError, Long> {

    /**
     * Find all errors for a specific import job
     */
    List<ImportJobError> findByImportJobIdOrderByRecordNumber(Long importJobId);

    /**
     * Find errors by category
     */
    List<ImportJobError> findByCategory(ImportJobError.ErrorCategory category);

    /**
     * Find errors by severity
     */
    List<ImportJobError> findBySeverity(ImportJobError.ErrorSeverity severity);

    /**
     * Find unresolved errors
     */
    List<ImportJobError> findByIsResolvedFalseOrderByErrorTimestampDesc();

    /**
     * Find unresolved errors for a specific job
     */
    List<ImportJobError> findByImportJobIdAndIsResolvedFalseOrderByRecordNumber(Long importJobId);

    /**
     * Find critical errors
     */
    List<ImportJobError> findBySeverityOrderByErrorTimestampDesc(ImportJobError.ErrorSeverity severity);

    /**
     * Find errors by type
     */
    List<ImportJobError> findByErrorTypeOrderByErrorTimestampDesc(String errorType);

    /**
     * Find resolvable errors (those with suggested fixes)
     */
    @Query("SELECT ije FROM ImportJobError ije WHERE ije.suggestedFix IS NOT NULL AND ije.suggestedFix != '' " +
           "AND ije.isResolved = false ORDER BY ije.errorTimestamp DESC")
    List<ImportJobError> findResolvableErrors();

    /**
     * Count errors by category for a specific job
     */
    @Query("SELECT ije.category, COUNT(ije) FROM ImportJobError ije WHERE ije.importJob.id = :jobId GROUP BY ije.category")
    List<Object[]> countErrorsByCategoryForJob(@Param("jobId") Long jobId);

    /**
     * Count errors by severity for a specific job
     */
    @Query("SELECT ije.severity, COUNT(ije) FROM ImportJobError ije WHERE ije.importJob.id = :jobId GROUP BY ije.severity")
    List<Object[]> countErrorsBySeverityForJob(@Param("jobId") Long jobId);

    /**
     * Find most common error types across all jobs
     */
    @Query("SELECT ije.errorType, COUNT(ije) as errorCount FROM ImportJobError ije " +
           "GROUP BY ije.errorType ORDER BY errorCount DESC")
    List<Object[]> findMostCommonErrorTypes();

    /**
     * Find errors affecting a specific field
     */
    List<ImportJobError> findByAffectedFieldOrderByErrorTimestampDesc(String affectedField);

    /**
     * Count unresolved errors for a job
     */
    long countByImportJobIdAndIsResolvedFalse(Long importJobId);

    /**
     * Count all unresolved errors
     */
    long countByIsResolvedFalse();

    /**
     * Count critical unresolved errors for a job
     */
    long countByImportJobIdAndSeverityAndIsResolvedFalse(Long importJobId, ImportJobError.ErrorSeverity severity);

    /**
     * Find errors created within date range
     */
    @Query("SELECT ije FROM ImportJobError ije WHERE ije.errorTimestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY ije.errorTimestamp DESC")
    List<ImportJobError> findErrorsCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Find errors by record number range for a job
     */
    @Query("SELECT ije FROM ImportJobError ije WHERE ije.importJob.id = :jobId " +
           "AND ije.recordNumber BETWEEN :startRecord AND :endRecord ORDER BY ije.recordNumber")
    List<ImportJobError> findErrorsByRecordRange(@Param("jobId") Long jobId, 
                                                @Param("startRecord") Long startRecord, 
                                                @Param("endRecord") Long endRecord);

    /**
     * Delete resolved errors older than specified date
     */
    @Query("DELETE FROM ImportJobError ije WHERE ije.isResolved = true AND ije.resolvedAt < :beforeDate")
    void deleteResolvedErrorsBefore(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Get error statistics for reporting
     */
    @Query("SELECT COUNT(ije) as totalErrors, " +
           "SUM(CASE WHEN ije.isResolved = true THEN 1 ELSE 0 END) as resolvedErrors, " +
           "SUM(CASE WHEN ije.severity = 'CRITICAL' THEN 1 ELSE 0 END) as criticalErrors " +
           "FROM ImportJobError ije WHERE ije.importJob.id = :jobId")
    Object[] getErrorStatisticsForJob(@Param("jobId") Long jobId);
}


