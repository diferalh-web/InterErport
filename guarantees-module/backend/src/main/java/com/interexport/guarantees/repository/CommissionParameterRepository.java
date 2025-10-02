package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.CommissionParameter;
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
 * Repository interface for CommissionParameter entity (F10 - Parameters Module)
 * Provides data access methods for commission parameter management
 */
@Repository
public interface CommissionParameterRepository extends JpaRepository<CommissionParameter, Long> {

    /**
     * Find commission parameters by guarantee type
     */
    List<CommissionParameter> findByGuaranteeTypeAndIsActiveTrue(GuaranteeType guaranteeType);

    /**
     * Find commission parameters by currency
     */
    List<CommissionParameter> findByCurrencyAndIsActiveTrue(String currency);

    /**
     * Find specific commission parameter for guarantee criteria
     */
    @Query("SELECT cp FROM CommissionParameter cp WHERE " +
           "cp.guaranteeType = :guaranteeType AND " +
           "cp.currency = :currency AND " +
           "cp.isDomestic = :isDomestic AND " +
           "cp.clientSegment = :clientSegment AND " +
           "cp.isActive = true AND " +
           "(cp.effectiveFrom IS NULL OR cp.effectiveFrom <= :date) AND " +
           "(cp.effectiveTo IS NULL OR cp.effectiveTo >= :date)")
    Optional<CommissionParameter> findActiveParameterForCriteria(
            @Param("guaranteeType") GuaranteeType guaranteeType,
            @Param("currency") String currency,
            @Param("isDomestic") Boolean isDomestic,
            @Param("clientSegment") String clientSegment,
            @Param("date") LocalDate date);

    /**
     * Find best matching commission parameter (fallback logic)
     */
    @Query("SELECT cp FROM CommissionParameter cp WHERE " +
           "cp.guaranteeType = :guaranteeType AND " +
           "cp.currency = :currency AND " +
           "cp.isDomestic = :isDomestic AND " +
           "cp.isActive = true AND " +
           "(cp.effectiveFrom IS NULL OR cp.effectiveFrom <= :date) AND " +
           "(cp.effectiveTo IS NULL OR cp.effectiveTo >= :date) " +
           "ORDER BY " +
           "CASE WHEN cp.clientSegment = :clientSegment THEN 0 ELSE 1 END, " +
           "cp.createdDate DESC")
    List<CommissionParameter> findBestMatchingParameters(
            @Param("guaranteeType") GuaranteeType guaranteeType,
            @Param("currency") String currency,
            @Param("isDomestic") Boolean isDomestic,
            @Param("clientSegment") String clientSegment,
            @Param("date") LocalDate date,
            Pageable pageable);

    /**
     * Find parameters by domestic/international flag
     */
    List<CommissionParameter> findByIsDomesticAndIsActiveTrue(Boolean isDomestic);

    /**
     * Find parameters by client segment
     */
    List<CommissionParameter> findByClientSegmentAndIsActiveTrue(String clientSegment);

    /**
     * Find expiring parameters
     */
    @Query("SELECT cp FROM CommissionParameter cp WHERE " +
           "cp.effectiveTo BETWEEN :startDate AND :endDate AND " +
           "cp.isActive = true")
    List<CommissionParameter> findParametersExpiringBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Search commission parameters
     */
    @Query("SELECT cp FROM CommissionParameter cp WHERE " +
           "LOWER(cp.currency) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(cp.clientSegment) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(cp.notes) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<CommissionParameter> searchParameters(@Param("search") String searchTerm, Pageable pageable);

    /**
     * Get parameters statistics
     */
    @Query("SELECT COUNT(cp) FROM CommissionParameter cp WHERE cp.isActive = true")
    long countActiveParameters();

    @Query("SELECT COUNT(cp) FROM CommissionParameter cp WHERE cp.guaranteeType = :type AND cp.isActive = true")
    long countActiveParametersByType(@Param("type") GuaranteeType guaranteeType);

    /**
     * Find duplicate parameters (validation)
     */
    @Query("SELECT COUNT(cp) > 0 FROM CommissionParameter cp WHERE " +
           "cp.guaranteeType = :guaranteeType AND " +
           "cp.currency = :currency AND " +
           "cp.isDomestic = :isDomestic AND " +
           "cp.clientSegment = :clientSegment AND " +
           "(:id IS NULL OR cp.id <> :id) AND " +
           "cp.isActive = true AND " +
           "((cp.effectiveFrom IS NULL AND :effectiveFrom IS NULL) OR " +
           " (cp.effectiveFrom <= :effectiveTo AND (cp.effectiveTo IS NULL OR cp.effectiveTo >= :effectiveFrom)))")
    boolean existsOverlappingParameter(
            @Param("guaranteeType") GuaranteeType guaranteeType,
            @Param("currency") String currency,
            @Param("isDomestic") Boolean isDomestic,
            @Param("clientSegment") String clientSegment,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("id") Long id);

    /**
     * Find all client segments
     */
    @Query("SELECT DISTINCT cp.clientSegment FROM CommissionParameter cp WHERE cp.isActive = true ORDER BY cp.clientSegment")
    List<String> findAllActiveClientSegments();

    /**
     * Find all currencies with commission parameters
     */
    @Query("SELECT DISTINCT cp.currency FROM CommissionParameter cp WHERE cp.isActive = true ORDER BY cp.currency")
    List<String> findAllActiveCurrencies();
}





