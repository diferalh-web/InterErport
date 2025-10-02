package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.Client;
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
 * Repository interface for Client entities.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Find client by unique client code
     */
    Optional<Client> findByClientCode(String clientCode);

    /**
     * Check if client code exists
     */
    boolean existsByClientCode(String clientCode);

    /**
     * Find clients by name (case-insensitive partial match)
     */
    List<Client> findByNameContainingIgnoreCase(String name);

    /**
     * Find clients by name with pagination
     */
    Page<Client> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find active clients
     */
    List<Client> findByIsActiveTrue();

    /**
     * Find active clients with pagination
     */
    Page<Client> findByIsActiveTrue(Pageable pageable);

    /**
     * Find clients by country
     */
    List<Client> findByCountryCode(String countryCode);

    /**
     * Find clients by risk rating
     */
    List<Client> findByRiskRating(String riskRating);

    /**
     * Find clients by entity type
     */
    List<Client> findByEntityType(String entityType);

    /**
     * Find clients with KYC review due
     */
    @Query("SELECT c FROM Client c WHERE " +
           "c.isActive = true AND " +
           "c.kycReviewDate IS NOT NULL AND " +
           "c.kycReviewDate <= CURRENT_DATE")
    List<Client> findWithKycReviewDue();

    /**
     * Find clients without KYC completion
     */
    List<Client> findByKycDateIsNull();

    /**
     * Find clients by industry code
     */
    List<Client> findByIndustryCode(String industryCode);

    /**
     * Find clients by credit currency
     */
    List<Client> findByCreditCurrency(String currency);

    /**
     * Find clients with credit limit above threshold
     */
    @Query("SELECT c FROM Client c WHERE " +
           "c.creditLimit IS NOT NULL AND c.creditLimit >= :threshold")
    List<Client> findWithCreditLimitAbove(@Param("threshold") java.math.BigDecimal threshold);

    /**
     * Search clients by multiple criteria
     */
    @Query("SELECT c FROM Client c WHERE " +
           "(:clientCode IS NULL OR UPPER(c.clientCode) LIKE UPPER(CONCAT('%', :clientCode, '%'))) AND " +
           "(:name IS NULL OR UPPER(c.name) LIKE UPPER(CONCAT('%', :name, '%'))) AND " +
           "(:countryCode IS NULL OR c.countryCode = :countryCode) AND " +
           "(:riskRating IS NULL OR c.riskRating = :riskRating) AND " +
           "(:isActive IS NULL OR c.isActive = :isActive)")
    Page<Client> findByCriteria(
            @Param("clientCode") String clientCode,
            @Param("name") String name,
            @Param("countryCode") String countryCode,
            @Param("riskRating") String riskRating,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    /**
     * Find clients with upcoming KYC review
     */
    @Query("SELECT c FROM Client c WHERE " +
           "c.isActive = true AND " +
           "c.kycReviewDate BETWEEN CURRENT_DATE AND :futureDate")
    List<Client> findWithUpcomingKycReview(@Param("futureDate") LocalDate futureDate);

    /**
     * Count active clients by country
     */
    @Query("SELECT c.countryCode, COUNT(c) FROM Client c WHERE " +
           "c.isActive = true GROUP BY c.countryCode")
    List<Object[]> countActiveClientsByCountry();
}
