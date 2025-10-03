package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GuaranteeContract entities.
 * Implements data access requirements from F1 - Guarantees CRUD.
 */
@Repository
public interface GuaranteeContractRepository extends JpaRepository<GuaranteeContract, Long> {

    /**
     * Find guarantee by unique business reference
     */
    Optional<GuaranteeContract> findByReference(String reference);

    /**
     * Find guarantee by SWIFT message reference
     */
    Optional<GuaranteeContract> findBySwiftMessageReference(String swiftMessageReference);

    /**
     * Check if reference exists (for uniqueness validation)
     */
    boolean existsByReference(String reference);

    /**
     * Find guarantees by status
     */
    List<GuaranteeContract> findByStatus(GuaranteeStatus status);

    /**
     * Find guarantees by status with pagination
     */
    Page<GuaranteeContract> findByStatus(GuaranteeStatus status, Pageable pageable);

    /**
     * Find guarantees by applicant (client)
     */
    List<GuaranteeContract> findByApplicantId(Long applicantId);

    /**
     * Find guarantees by applicant with pagination
     */
    Page<GuaranteeContract> findByApplicantId(Long applicantId, Pageable pageable);

    /**
     * Find guarantees by type
     */
    List<GuaranteeContract> findByGuaranteeType(GuaranteeType guaranteeType);

    /**
     * Find guarantees by currency
     */
    List<GuaranteeContract> findByCurrency(String currency);

    /**
     * Find guarantees by currency with pagination
     */
    Page<GuaranteeContract> findByCurrency(String currency, Pageable pageable);

    /**
     * Find guarantees expiring within a date range
     */
    List<GuaranteeContract> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find guarantees expiring within a date range ordered by expiry date ascending
     */
    List<GuaranteeContract> findByExpiryDateBetweenOrderByExpiryDateAsc(LocalDate startDate, LocalDate endDate);

    /**
     * Find guarantees expiring on or before a specific date
     */
    List<GuaranteeContract> findByExpiryDateLessThanEqual(LocalDate expiryDate);

    /**
     * Find guarantees issued within a date range
     */
    List<GuaranteeContract> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find guarantees by multiple criteria with pagination
     */
    @Query("SELECT g FROM GuaranteeContract g WHERE " +
           "(:reference IS NULL OR g.reference LIKE %:reference%) AND " +
           "(:status IS NULL OR g.status = :status) AND " +
           "(:guaranteeType IS NULL OR g.guaranteeType = :guaranteeType) AND " +
           "(:applicantId IS NULL OR g.applicantId = :applicantId) AND " +
           "(:currency IS NULL OR g.currency = :currency) AND " +
           "(:fromDate IS NULL OR g.issueDate >= :fromDate) AND " +
           "(:toDate IS NULL OR g.issueDate <= :toDate)")
    Page<GuaranteeContract> findByCriteria(
            @Param("reference") String reference,
            @Param("status") GuaranteeStatus status,
            @Param("guaranteeType") GuaranteeType guaranteeType,
            @Param("applicantId") Long applicantId,
            @Param("currency") String currency,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    /**
     * Find guarantees by creation date range and status for reporting
     */
    List<GuaranteeContract> findByCreatedDateBetweenAndStatusOrderByCreatedDateDesc(
            java.time.LocalDateTime fromDate, 
            java.time.LocalDateTime toDate, 
            GuaranteeStatus status);

    /**
     * Find guarantees by creation date range for reporting
     */
    List<GuaranteeContract> findByCreatedDateBetweenOrderByCreatedDateDesc(
            java.time.LocalDateTime fromDate, 
            java.time.LocalDateTime toDate);

    /**
     * Find guarantees with active claims
     */
    @Query("SELECT DISTINCT g FROM GuaranteeContract g " +
           "INNER JOIN g.claims c " +
           "WHERE c.status IN ('REQUESTED', 'UNDER_REVIEW', 'PENDING_DOCUMENTS', 'APPROVED')")
    List<GuaranteeContract> findWithActiveClaims();

    /**
     * Find guarantees expiring soon (for alerts)
     */
    @Query("SELECT g FROM GuaranteeContract g " +
           "WHERE g.status = 'APPROVED' " +
           "AND g.expiryDate BETWEEN :startDate AND :endDate " +
           "ORDER BY g.expiryDate ASC")
    List<GuaranteeContract> findExpiringSoon(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find guarantees by beneficiary name (partial match)
     */
    @Query("SELECT g FROM GuaranteeContract g " +
           "WHERE UPPER(g.beneficiaryName) LIKE UPPER(CONCAT('%', :beneficiaryName, '%'))")
    List<GuaranteeContract> findByBeneficiaryNameContainingIgnoreCase(
            @Param("beneficiaryName") String beneficiaryName);

    /**
     * Count guarantees by status
     */
    long countByStatus(GuaranteeStatus status);

    /**
     * Count guarantees by applicant
     */
    long countByApplicantId(Long applicantId);

    /**
     * Get total outstanding amount by currency
     */
    @Query("SELECT g.currency, SUM(g.amount) " +
           "FROM GuaranteeContract g " +
           "WHERE g.status = 'APPROVED' " +
           "GROUP BY g.currency")
    List<Object[]> getTotalOutstandingAmountByCurrency();

    /**
     * Get total outstanding amount in base currency
     */
    @Query("SELECT SUM(g.baseAmount) " +
           "FROM GuaranteeContract g " +
           "WHERE g.status = 'APPROVED'")
    java.math.BigDecimal getTotalOutstandingAmountInBaseCurrency();

    /**
     * Find guarantees by underlying contract reference
     */
    List<GuaranteeContract> findByUnderlyingContractRef(String underlyingContractRef);

    /**
     * Find domestic guarantees
     */
    List<GuaranteeContract> findByIsDomestic(Boolean isDomestic);

    /**
     * Find guarantees by advising bank
     */
    List<GuaranteeContract> findByAdvisingBankBic(String advisingBankBic);

    /**
     * Custom query for complex reporting needs
     */
    @Query("SELECT g FROM GuaranteeContract g " +
           "LEFT JOIN FETCH g.amendments a " +
           "LEFT JOIN FETCH g.claims c " +
           "LEFT JOIN FETCH g.feeItems f " +
           "WHERE g.id = :id")
    Optional<GuaranteeContract> findByIdWithDetails(@Param("id") Long id);

    /**
     * Find guarantees needing review (drafts older than threshold)
     */
    @Query("SELECT g FROM GuaranteeContract g " +
           "WHERE g.status = 'DRAFT' " +
           "AND g.createdDate < :threshold")
    List<GuaranteeContract> findDraftsOlderThan(@Param("threshold") java.time.LocalDateTime threshold);

    /**
     * Search guarantees by text in multiple fields
     */
    @Query("SELECT g FROM GuaranteeContract g WHERE " +
           "UPPER(g.reference) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(g.beneficiaryName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(g.guaranteeText) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
           "UPPER(g.underlyingContractRef) LIKE UPPER(CONCAT('%', :searchTerm, '%'))")
    Page<GuaranteeContract> searchByText(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Dashboard Analytics Methods
     */

    /**
     * Get total guarantee amount across all guarantees
     */
    @Query("SELECT SUM(g.amount) FROM GuaranteeContract g")
    java.math.BigDecimal getTotalGuaranteeAmount();

    /**
     * Count guarantees by multiple statuses
     */
    long countByStatusIn(List<GuaranteeStatus> statuses);

    /**
     * Get guarantee statistics by date range for monthly reports
     */
    @Query("SELECT COUNT(g), COALESCE(SUM(g.amount), 0) " +
           "FROM GuaranteeContract g " +
           "WHERE CAST(g.createdDate AS DATE) BETWEEN :startDate AND :endDate")
    List<Object[]> getGuaranteeStatsByDateRange(@Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);

    /**
     * Get total amount by currency for all guarantees
     */
    @Query("SELECT g.currency, SUM(g.amount), COUNT(g) " +
           "FROM GuaranteeContract g " +
           "GROUP BY g.currency " +
           "ORDER BY SUM(g.amount) DESC")
    List<Object[]> getTotalAmountByCurrency();

    /**
     * Get daily activity trend for guarantees
     */
    @Query("SELECT CAST(g.createdDate AS DATE), COUNT(g) " +
           "FROM GuaranteeContract g " +
           "WHERE CAST(g.createdDate AS DATE) BETWEEN :startDate AND :endDate " +
           "GROUP BY CAST(g.createdDate AS DATE) " +
           "ORDER BY CAST(g.createdDate AS DATE)")
    List<Object[]> getDailyActivityTrend(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);
}
