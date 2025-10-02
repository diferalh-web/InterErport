package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.FeeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for FeeItem entities.
 */
@Repository
public interface FeeItemRepository extends JpaRepository<FeeItem, Long> {

    /**
     * Find fee items by guarantee ID
     */
    List<FeeItem> findByGuaranteeIdOrderByCreatedDateDesc(Long guaranteeId);

    /**
     * Find fee items by type
     */
    List<FeeItem> findByFeeType(String feeType);

    /**
     * Find fee items by payment status
     */
    List<FeeItem> findByIsPaid(Boolean isPaid);

    /**
     * Find unpaid fee items
     */
    List<FeeItem> findByIsPaidFalse();

    /**
     * Find overdue fee items
     */
    @Query("SELECT f FROM FeeItem f WHERE " +
           "f.isPaid = false AND " +
           "f.dueDate IS NOT NULL AND " +
           "f.dueDate < CURRENT_DATE")
    List<FeeItem> findOverdueFees();

    /**
     * Find fee items due within date range
     */
    List<FeeItem> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find fee items by currency
     */
    List<FeeItem> findByCurrency(String currency);

    /**
     * Find installment fee items for a guarantee
     */
    List<FeeItem> findByGuaranteeIdAndInstallmentNumberIsNotNull(Long guaranteeId);

    /**
     * Get total unpaid fees for a guarantee
     */
    @Query("SELECT SUM(f.amount) FROM FeeItem f WHERE " +
           "f.guarantee.id = :guaranteeId AND f.isPaid = false")
    BigDecimal getTotalUnpaidFeesForGuarantee(@Param("guaranteeId") Long guaranteeId);

    /**
     * Get total fees by type
     */
    @Query("SELECT SUM(f.baseAmount) FROM FeeItem f WHERE " +
           "f.feeType = :feeType AND f.isPaid = true")
    BigDecimal getTotalPaidFeesByType(@Param("feeType") String feeType);

    /**
     * Find fee items by accounting entry reference
     */
    List<FeeItem> findByAccountingEntryRef(String accountingEntryRef);
}

