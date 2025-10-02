package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.Claim;
import com.interexport.guarantees.entity.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Claim entities.
 */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /**
     * Find claim by unique reference
     */
    Optional<Claim> findByClaimReference(String claimReference);

    /**
     * Find claims by guarantee ID
     */
    List<Claim> findByGuaranteeIdOrderByClaimDateDesc(Long guaranteeId);

    /**
     * Find claims by guarantee ID with pagination
     */
    Page<Claim> findByGuaranteeId(Long guaranteeId, Pageable pageable);

    /**
     * Find claims by status
     */
    List<Claim> findByStatus(ClaimStatus status);

    /**
     * Find claims by status with pagination
     */
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);

    /**
     * Find active claims (not settled or rejected)
     */
    @Query("SELECT c FROM Claim c WHERE c.status NOT IN ('SETTLED', 'REJECTED')")
    List<Claim> findActiveClaims();

    /**
     * Find claims by processing deadline
     */
    List<Claim> findByProcessingDeadlineLessThanEqual(LocalDate deadline);

    /**
     * Find overdue claims
     */
    @Query("SELECT c FROM Claim c WHERE " +
           "c.processingDeadline IS NOT NULL AND " +
           "c.processingDeadline < CURRENT_DATE AND " +
           "c.status NOT IN ('SETTLED', 'REJECTED')")
    List<Claim> findOverdueClaims();

    /**
     * Find claims by SWIFT message reference
     */
    Optional<Claim> findBySwiftMessageReference(String swiftMessageReference);

    /**
     * Find claims requiring special approval
     */
    List<Claim> findByRequiresSpecialApprovalTrueAndStatus(ClaimStatus status);

    /**
     * Find claims with missing documents
     */
    @Query("SELECT c FROM Claim c WHERE " +
           "c.status = 'PENDING_DOCUMENTS' AND " +
           "c.missingDocuments IS NOT NULL")
    List<Claim> findWithMissingDocuments();

    /**
     * Get total claimed amount for a guarantee
     */
    @Query("SELECT SUM(c.amount) FROM Claim c WHERE " +
           "c.guarantee.id = :guaranteeId AND c.status = 'SETTLED'")
    BigDecimal getTotalClaimedAmountForGuarantee(@Param("guaranteeId") Long guaranteeId);

    /**
     * Count claims by status
     */
    long countByStatus(ClaimStatus status);

    /**
     * Find claims within date range
     */
    List<Claim> findByClaimDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find claims approved by user
     */
    List<Claim> findByApprovedBy(String approvedBy);

    /**
     * Find claims rejected by user
     */
    List<Claim> findByRejectedBy(String rejectedBy);

    /**
     * Find claims by guarantee ID and status
     */
    List<Claim> findByGuaranteeIdAndStatus(Long guaranteeId, ClaimStatus status);

    /**
     * Find claims by guarantee ID and amount between range
     */
    List<Claim> findByGuaranteeIdAndAmountBetween(Long guaranteeId, BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Find claims by guarantee ID ordered by claim date with pagination
     */
    Page<Claim> findByGuaranteeIdOrderByClaimDateDesc(Long guaranteeId, Pageable pageable);

    /**
     * Find claims by guarantee ID with multiple statuses
     */
    @Query("SELECT c FROM Claim c WHERE c.guarantee.id = :guaranteeId AND c.status IN :statuses")
    List<Claim> findByGuaranteeIdAndStatusIn(@Param("guaranteeId") Long guaranteeId, @Param("statuses") java.util.List<ClaimStatus> statuses);

    /**
     * Find claims by guarantee ID (simple version without pagination)
     */
    List<Claim> findByGuaranteeId(Long guaranteeId);

    /**
     * Dashboard Analytics Methods
     */

    /**
     * Get total claim amount across all claims
     */
    @Query("SELECT SUM(c.amount) FROM Claim c")
    java.math.BigDecimal getTotalClaimAmount();

    /**
     * Get claim statistics by date range for monthly reports
     */
    @Query("SELECT COUNT(c), COALESCE(SUM(c.amount), 0) " +
           "FROM Claim c " +
           "WHERE c.claimDate BETWEEN :startDate AND :endDate")
    List<Object[]> getClaimStatsByDateRange(@Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);

    /**
     * Get total amount by currency for all claims
     */
    @Query("SELECT c.currency, SUM(c.amount), COUNT(c) " +
           "FROM Claim c " +
           "GROUP BY c.currency " +
           "ORDER BY SUM(c.amount) DESC")
    List<Object[]> getTotalAmountByCurrency();

    /**
     * Get daily activity trend for claims
     */
    @Query("SELECT CAST(c.createdDate AS DATE), COUNT(c) " +
           "FROM Claim c " +
           "WHERE CAST(c.createdDate AS DATE) BETWEEN :startDate AND :endDate " +
           "GROUP BY CAST(c.createdDate AS DATE) " +
           "ORDER BY CAST(c.createdDate AS DATE)")
    List<Object[]> getDailyActivityTrend(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);
}
