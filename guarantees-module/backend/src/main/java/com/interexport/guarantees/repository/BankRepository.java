package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.Bank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Bank entity (F10 - Parameters Module)
 * Provides data access methods for bank management
 */
@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {

    /**
     * Find bank by BIC code
     */
    Optional<Bank> findByBicCode(String bicCode);

    /**
     * Find banks by name (case-insensitive)
     */
    List<Bank> findByNameContainingIgnoreCase(String name);

    /**
     * Find active banks
     */
    List<Bank> findByIsActiveTrue();

    /**
     * Find correspondent banks
     */
    List<Bank> findByIsCorrespondentTrue();

    /**
     * Find active correspondent banks
     */
    @Query("SELECT b FROM Bank b WHERE b.isActive = true AND b.isCorrespondent = true")
    List<Bank> findActiveCorrespondentBanks();

    /**
     * Find banks by country
     */
    List<Bank> findByCountryCodeOrderByName(String countryCode);

    /**
     * Find banks by type
     */
    List<Bank> findByBankTypeOrderByName(Bank.BankType bankType);

    /**
     * Search banks by name, BIC or country
     */
    @Query("SELECT b FROM Bank b WHERE " +
           "LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.bicCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.countryCode) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Bank> searchBanks(@Param("search") String searchTerm, Pageable pageable);

    /**
     * Find banks with accounts in specific currency
     */
    @Query("SELECT DISTINCT b FROM Bank b JOIN b.accounts a WHERE a.currency = :currency AND a.isActive = true")
    List<Bank> findBanksWithActiveAccountsInCurrency(@Param("currency") String currency);

    /**
     * Get bank statistics
     */
    @Query("SELECT COUNT(b) FROM Bank b WHERE b.isActive = true")
    long countActiveBanks();

    @Query("SELECT COUNT(b) FROM Bank b WHERE b.isCorrespondent = true AND b.isActive = true")
    long countActiveCorrespondentBanks();

    /**
     * Check if BIC code exists (excluding specific bank ID)
     */
    @Query("SELECT COUNT(b) > 0 FROM Bank b WHERE b.bicCode = :bicCode AND (:bankId IS NULL OR b.id <> :bankId)")
    boolean existsByBicCodeAndIdNot(@Param("bicCode") String bicCode, @Param("bankId") Long bankId);
}





