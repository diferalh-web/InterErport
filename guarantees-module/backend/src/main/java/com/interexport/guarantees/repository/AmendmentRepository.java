package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.Amendment;
import com.interexport.guarantees.entity.enums.AmendmentType;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Amendment entity (F5)
 * Provides data access methods for amendment management
 */
@Repository
public interface AmendmentRepository extends JpaRepository<Amendment, Long> {

    /**
     * Find amendments by guarantee ID ordered by creation date (most recent first)
     */
    Page<Amendment> findByGuaranteeIdOrderByCreatedDateDesc(Long guaranteeId, Pageable pageable);

    /**
     * Find amendments by guarantee ID
     */
    List<Amendment> findByGuaranteeId(Long guaranteeId);

    /**
     * Find amendments by guarantee ID and status
     */
    List<Amendment> findByGuaranteeIdAndStatus(Long guaranteeId, GuaranteeStatus status);

    /**
     * Find amendments by guarantee ID and amendment type
     */
    List<Amendment> findByGuaranteeIdAndAmendmentType(Long guaranteeId, AmendmentType amendmentType);

    /**
     * Find amendment by reference
     */
    Optional<Amendment> findByAmendmentReference(String amendmentReference);

    /**
     * Find amendments requiring consent that haven't received it
     */
    @Query("SELECT a FROM Amendment a WHERE " +
           "a.requiresConsent = true AND " +
           "a.consentReceivedDate IS NULL AND " +
           "a.status IN ('DRAFT', 'SUBMITTED')")
    List<Amendment> findAmendmentsPendingConsent();

    /**
     * Find amendments by status across all guarantees
     */
    List<Amendment> findByStatusOrderBySubmittedDateDesc(GuaranteeStatus status);

    /**
     * Find amendments submitted within date range
     */
    @Query("SELECT a FROM Amendment a WHERE " +
           "a.submittedDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.submittedDate DESC")
    List<Amendment> findAmendmentsSubmittedBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count amendments by guarantee
     */
    long countByGuaranteeId(Long guaranteeId);

    /**
     * Count amendments by status for a guarantee
     */
    long countByGuaranteeIdAndStatus(Long guaranteeId, GuaranteeStatus status);

    /**
     * Find amendments processed by specific user
     */
    List<Amendment> findByProcessedByOrderByProcessedDateDesc(String processedBy);

    /**
     * Search amendments by description or reason
     */
    @Query("SELECT a FROM Amendment a WHERE " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.reason) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.amendmentReference) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Amendment> searchAmendments(@Param("search") String searchTerm, Pageable pageable);

    /**
     * Find amendments with consent received within date range
     */
    @Query("SELECT a FROM Amendment a WHERE " +
           "a.consentReceivedDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.consentReceivedDate DESC")
    List<Amendment> findAmendmentsWithConsentBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Check if amendment reference exists (for uniqueness validation)
     */
    boolean existsByAmendmentReference(String amendmentReference);

    /**
     * Get amendment statistics
     */
    @Query("SELECT COUNT(a) FROM Amendment a WHERE a.status = :status")
    long countByStatus(@Param("status") GuaranteeStatus status);

    @Query("SELECT COUNT(a) FROM Amendment a WHERE a.amendmentType = :type")
    long countByAmendmentType(@Param("type") AmendmentType amendmentType);

    /**
     * Dashboard Analytics Methods
     */

    /**
     * Get amendment statistics by date range for monthly reports
     */
    @Query("SELECT COUNT(a) " +
           "FROM Amendment a " +
           "WHERE CAST(a.createdDate AS DATE) BETWEEN :startDate AND :endDate")
    List<Object[]> getAmendmentStatsByDateRange(@Param("startDate") java.time.LocalDate startDate, 
                                              @Param("endDate") java.time.LocalDate endDate);

    /**
     * Get daily activity trend for amendments
     */
    @Query("SELECT CAST(a.createdDate AS DATE), COUNT(a) " +
           "FROM Amendment a " +
           "WHERE CAST(a.createdDate AS DATE) BETWEEN :startDate AND :endDate " +
           "GROUP BY CAST(a.createdDate AS DATE) " +
           "ORDER BY CAST(a.createdDate AS DATE)")
    List<Object[]> getDailyActivityTrend(@Param("startDate") java.time.LocalDate startDate, 
                                       @Param("endDate") java.time.LocalDate endDate);
}