package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Account entity (F10 - Parameters Module)
 * Provides data access methods for account management
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find account by bank and account number
     */
    Optional<Account> findByBankIdAndAccountNumber(Long bankId, String accountNumber);

    /**
     * Find accounts by bank
     */
    List<Account> findByBankIdOrderByAccountName(Long bankId);

    /**
     * Find active accounts by bank
     */
    List<Account> findByBankIdAndIsActiveTrueOrderByAccountName(Long bankId);

    /**
     * Find accounts by currency
     */
    List<Account> findByCurrencyOrderByAccountName(String currency);

    /**
     * Find active accounts by currency
     */
    List<Account> findByCurrencyAndIsActiveTrueOrderByAccountName(String currency);

    /**
     * Find accounts by type
     */
    List<Account> findByAccountTypeOrderByAccountName(Account.AccountType accountType);

    /**
     * Find accounts by GL code
     */
    List<Account> findByGlCodeContaining(String glCode);

    /**
     * Find default account for currency and type
     */
    @Query("SELECT a FROM Account a WHERE a.currency = :currency AND a.accountType = :type AND a.isDefault = true AND a.isActive = true")
    Optional<Account> findDefaultAccountByCurrencyAndType(@Param("currency") String currency, 
                                                         @Param("type") Account.AccountType type);

    /**
     * Search accounts by name, number, or GL code
     */
    @Query("SELECT a FROM Account a WHERE " +
           "LOWER(a.accountName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.glCode) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Account> searchAccounts(@Param("search") String searchTerm, Pageable pageable);

    /**
     * Find accounts with balance above threshold
     */
    @Query("SELECT a FROM Account a WHERE a.balance > :threshold AND a.isActive = true")
    List<Account> findAccountsWithBalanceAbove(@Param("threshold") BigDecimal threshold);

    /**
     * Find accounts by bank BIC code
     */
    @Query("SELECT a FROM Account a JOIN a.bank b WHERE b.bicCode = :bicCode AND a.isActive = true")
    List<Account> findActiveAccountsByBankBic(@Param("bicCode") String bicCode);

    /**
     * Get account statistics
     */
    @Query("SELECT COUNT(a) FROM Account a WHERE a.isActive = true")
    long countActiveAccounts();

    @Query("SELECT COUNT(a) FROM Account a WHERE a.currency = :currency AND a.isActive = true")
    long countActiveAccountsByCurrency(@Param("currency") String currency);

    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.currency = :currency AND a.isActive = true")
    BigDecimal getTotalBalanceByCurrency(@Param("currency") String currency);

    /**
     * Find liability accounts for guarantee processing
     */
    @Query("SELECT a FROM Account a WHERE a.accountType = 'LIABILITY' AND a.currency = :currency AND a.isActive = true")
    List<Account> findLiabilityAccountsByCurrency(@Param("currency") String currency);

    /**
     * Check for duplicate account within bank
     */
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.bank.id = :bankId AND a.accountNumber = :accountNumber AND (:accountId IS NULL OR a.id <> :accountId)")
    boolean existsByBankIdAndAccountNumberAndIdNot(@Param("bankId") Long bankId, 
                                                  @Param("accountNumber") String accountNumber, 
                                                  @Param("accountId") Long accountId);
}





