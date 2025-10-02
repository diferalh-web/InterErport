package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.FxRate;
import com.interexport.guarantees.entity.enums.FxRateProvider;
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
 * Repository interface for FxRate entities.
 */
@Repository
public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    /**
     * Find current active rate for currency pair
     */
    @Query("SELECT f FROM FxRate f WHERE " +
           "f.baseCurrency = :baseCurrency AND " +
           "f.targetCurrency = :targetCurrency AND " +
           "f.isActive = true AND " +
           "f.effectiveDate <= CURRENT_DATE AND " +
           "(f.expiryDate IS NULL OR f.expiryDate >= CURRENT_DATE) " +
           "ORDER BY f.effectiveDate DESC")
    Optional<FxRate> findCurrentRate(
            @Param("baseCurrency") String baseCurrency,
            @Param("targetCurrency") String targetCurrency);

    /**
     * Find rate for specific date
     */
    @Query("SELECT f FROM FxRate f WHERE " +
           "f.baseCurrency = :baseCurrency AND " +
           "f.targetCurrency = :targetCurrency AND " +
           "f.isActive = true AND " +
           "f.effectiveDate <= :date AND " +
           "(f.expiryDate IS NULL OR f.expiryDate >= :date) " +
           "ORDER BY f.effectiveDate DESC")
    Optional<FxRate> findRateForDate(
            @Param("baseCurrency") String baseCurrency,
            @Param("targetCurrency") String targetCurrency,
            @Param("date") LocalDate date);

    /**
     * Find all rates for a currency pair
     */
    List<FxRate> findByBaseCurrencyAndTargetCurrencyOrderByEffectiveDateDesc(
            String baseCurrency, String targetCurrency);

    /**
     * Find rates by base currency
     */
    List<FxRate> findByBaseCurrency(String baseCurrency);

    /**
     * Find rates by target currency
     */
    List<FxRate> findByTargetCurrency(String targetCurrency);

    /**
     * Find rates by provider
     */
    List<FxRate> findByProvider(FxRateProvider provider);

    /**
     * Find active rates
     */
    List<FxRate> findByIsActiveTrue();

    /**
     * Find active rates with pagination
     */
    Page<FxRate> findByIsActiveTrue(Pageable pageable);

    /**
     * Find expired rates
     */
    @Query("SELECT f FROM FxRate f WHERE " +
           "f.expiryDate IS NOT NULL AND f.expiryDate < CURRENT_DATE")
    List<FxRate> findExpiredRates();

    /**
     * Find rates effective on specific date
     */
    List<FxRate> findByEffectiveDate(LocalDate effectiveDate);

    /**
     * Find rates within date range
     */
    List<FxRate> findByEffectiveDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find rates by provider reference
     */
    Optional<FxRate> findByProviderReference(String providerReference);

    /**
     * Find all current active rates (for cache refresh)
     */
    @Query("SELECT f FROM FxRate f WHERE " +
           "f.isActive = true AND " +
           "f.effectiveDate <= CURRENT_DATE AND " +
           "(f.expiryDate IS NULL OR f.expiryDate >= CURRENT_DATE)")
    List<FxRate> findAllCurrentActiveRates();

    /**
     * Find duplicate rates (same currency pair, effective date)
     */
    @Query("SELECT f FROM FxRate f WHERE " +
           "f.baseCurrency = :baseCurrency AND " +
           "f.targetCurrency = :targetCurrency AND " +
           "f.effectiveDate = :effectiveDate AND " +
           "f.id <> :excludeId")
    List<FxRate> findDuplicateRates(
            @Param("baseCurrency") String baseCurrency,
            @Param("targetCurrency") String targetCurrency,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("excludeId") Long excludeId);

    /**
     * Find rates needing refresh (older than threshold for API providers)
     */
    @Query("SELECT f FROM FxRate f WHERE " +
           "f.provider <> 'MANUAL' AND " +
           "f.isActive = true AND " +
           "f.retrievedAt < :threshold")
    List<FxRate> findRatesNeedingRefresh(
            @Param("threshold") java.time.LocalDateTime threshold);

    /**
     * Get distinct currency pairs
     */
    @Query("SELECT DISTINCT f.baseCurrency, f.targetCurrency FROM FxRate f WHERE f.isActive = true")
    List<Object[]> findDistinctCurrencyPairs();

    /**
     * Find latest rate for each currency pair
     */
    @Query("SELECT f FROM FxRate f WHERE f.id IN (" +
           "SELECT MAX(f2.id) FROM FxRate f2 WHERE " +
           "f2.baseCurrency = f.baseCurrency AND " +
           "f2.targetCurrency = f.targetCurrency AND " +
           "f2.isActive = true " +
           "GROUP BY f2.baseCurrency, f2.targetCurrency)")
    List<FxRate> findLatestRateForEachPair();

    /**
     * Find all supported currencies (both base and target)
     */
    @Query("SELECT DISTINCT currency FROM (" +
           "SELECT f.baseCurrency as currency FROM FxRate f WHERE f.isActive = true " +
           "UNION " +
           "SELECT f.targetCurrency as currency FROM FxRate f WHERE f.isActive = true" +
           ") ORDER BY currency")
    List<String> findAllSupportedCurrencies();
}
