package com.interexport.guarantees.repository.query;

import com.interexport.guarantees.cqrs.query.GuaranteeSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for GuaranteeSummaryView (Query Side)
 * Optimized for read operations
 */
@Repository
public interface GuaranteeSummaryViewRepository extends JpaRepository<GuaranteeSummaryView, String> {
    
    /**
     * Find guarantees by status
     */
    List<GuaranteeSummaryView> findByStatus(String status);
    
    /**
     * Find guarantees by currency
     */
    List<GuaranteeSummaryView> findByCurrency(String currency);
    
    /**
     * Find guarantees expiring between two dates with specific status
     */
    List<GuaranteeSummaryView> findByExpiryDateBetweenAndStatus(LocalDate startDate, LocalDate endDate, String status);
    
    /**
     * Find guarantees by guarantee type
     */
    List<GuaranteeSummaryView> findByGuaranteeType(String guaranteeType);
    
    /**
     * Find guarantees by risk level
     */
    List<GuaranteeSummaryView> findByRiskLevel(String riskLevel);
    
    /**
     * Find guarantees by applicant name (denormalized field)
     */
    List<GuaranteeSummaryView> findByApplicantNameContainingIgnoreCase(String applicantName);
    
    /**
     * Find guarantees by beneficiary name
     */
    List<GuaranteeSummaryView> findByBeneficiaryNameContainingIgnoreCase(String beneficiaryName);
    
    /**
     * Get dashboard summary statistics
     */
    @Query("SELECT " +
           "COUNT(g) as totalGuarantees, " +
           "SUM(g.amount) as totalAmount, " +
           "AVG(g.amount) as averageAmount, " +
           "COUNT(CASE WHEN g.status = 'ACTIVE' THEN 1 END) as activeGuarantees, " +
           "COUNT(CASE WHEN g.status = 'EXPIRED' THEN 1 END) as expiredGuarantees " +
           "FROM GuaranteeSummaryView g")
    Object[] getDashboardSummary();
    
    /**
     * Get guarantees by amount range
     */
    @Query("SELECT g FROM GuaranteeSummaryView g WHERE g.amount BETWEEN :minAmount AND :maxAmount")
    List<GuaranteeSummaryView> findByAmountRange(@Param("minAmount") java.math.BigDecimal minAmount, 
                                                @Param("maxAmount") java.math.BigDecimal maxAmount);
    
    /**
     * Get top guarantees by amount
     */
    @Query("SELECT g FROM GuaranteeSummaryView g ORDER BY g.amount DESC")
    List<GuaranteeSummaryView> findTopGuaranteesByAmount(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Get guarantees expiring soon (custom query)
     */
    @Query("SELECT g FROM GuaranteeSummaryView g WHERE g.expiryDate BETWEEN :today AND :futureDate AND g.status = 'ACTIVE' ORDER BY g.expiryDate ASC")
    List<GuaranteeSummaryView> findExpiringGuarantees(@Param("today") LocalDate today, 
                                                     @Param("futureDate") LocalDate futureDate);
    
    /**
     * Get guarantees by multiple criteria
     */
    @Query("SELECT g FROM GuaranteeSummaryView g WHERE " +
           "(:status IS NULL OR g.status = :status) AND " +
           "(:currency IS NULL OR g.currency = :currency) AND " +
           "(:guaranteeType IS NULL OR g.guaranteeType = :guaranteeType) AND " +
           "(:riskLevel IS NULL OR g.riskLevel = :riskLevel)")
    List<GuaranteeSummaryView> findByMultipleCriteria(@Param("status") String status,
                                                     @Param("currency") String currency,
                                                     @Param("guaranteeType") String guaranteeType,
                                                     @Param("riskLevel") String riskLevel);
}
