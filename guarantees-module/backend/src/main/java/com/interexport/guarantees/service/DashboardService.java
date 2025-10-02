package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.repository.AmendmentRepository;
import com.interexport.guarantees.repository.ClaimRepository;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for dashboard metrics and analytics
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final GuaranteeContractRepository guaranteeRepository;
    private final ClaimRepository claimRepository;
    private final AmendmentRepository amendmentRepository;

    @Autowired
    public DashboardService(GuaranteeContractRepository guaranteeRepository,
                          ClaimRepository claimRepository,
                          AmendmentRepository amendmentRepository) {
        this.guaranteeRepository = guaranteeRepository;
        this.claimRepository = claimRepository;
        this.amendmentRepository = amendmentRepository;
    }

    /**
     * Get dashboard summary with total amounts and counts
     * Cached in Redis for 5 minutes to improve performance
     */
    @Cacheable(value = "dashboard", key = "'summary'", cacheManager = "cacheManager")
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Guarantee metrics
        Long totalGuarantees = guaranteeRepository.count();
        BigDecimal totalGuaranteeAmount = guaranteeRepository.getTotalGuaranteeAmount();
        Long activeGuarantees = guaranteeRepository.countByStatusIn(Arrays.asList(
            GuaranteeStatus.SUBMITTED, GuaranteeStatus.APPROVED));

        // Claim metrics
        Long totalClaims = claimRepository.count();
        BigDecimal totalClaimAmount = claimRepository.getTotalClaimAmount();
        
        // Amendment metrics  
        Long totalAmendments = amendmentRepository.count();

        summary.put("guarantees", Map.of(
            "total", totalGuarantees,
            "totalAmount", totalGuaranteeAmount != null ? totalGuaranteeAmount : BigDecimal.ZERO,
            "active", activeGuarantees
        ));

        summary.put("claims", Map.of(
            "total", totalClaims,
            "totalAmount", totalClaimAmount != null ? totalClaimAmount : BigDecimal.ZERO
        ));

        summary.put("amendments", Map.of(
            "total", totalAmendments
        ));

        return summary;
    }

    /**
     * Get monthly statistics for the last N months
     * Cached in Redis for 5 minutes to improve performance
     */
    @Cacheable(value = "dashboard", key = "'monthly_' + #monthsBack", cacheManager = "cacheManager")
    public Map<String, Object> getMonthlyStatistics(int monthsBack) {
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        
        LocalDate startDate = LocalDate.now().minusMonths(monthsBack).withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();

        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        while (!currentMonth.isAfter(endMonth)) {
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            // Get guarantees for this month
            List<Object[]> guaranteeStats = guaranteeRepository.getGuaranteeStatsByDateRange(monthStart, monthEnd);
            List<Object[]> claimStats = claimRepository.getClaimStatsByDateRange(monthStart, monthEnd);
            List<Object[]> amendmentStats = amendmentRepository.getAmendmentStatsByDateRange(monthStart, monthEnd);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", currentMonth.toString());
            monthData.put("monthLabel", currentMonth.getMonth().name() + " " + currentMonth.getYear());

            // Process guarantee stats
            BigDecimal guaranteeAmount = BigDecimal.ZERO;
            Long guaranteeCount = 0L;
            if (!guaranteeStats.isEmpty()) {
                Object[] stats = guaranteeStats.get(0);
                guaranteeCount = (Long) stats[0];
                guaranteeAmount = (BigDecimal) stats[1];
            }

            // Process claim stats
            BigDecimal claimAmount = BigDecimal.ZERO;
            Long claimCount = 0L;
            if (!claimStats.isEmpty()) {
                Object[] stats = claimStats.get(0);
                claimCount = (Long) stats[0];
                claimAmount = (BigDecimal) stats[1];
            }

            // Process amendment stats
            Long amendmentCount = 0L;
            if (!amendmentStats.isEmpty()) {
                Object[] stats = amendmentStats.get(0);
                amendmentCount = (Long) stats[0];
            }

            monthData.put("guarantees", Map.of(
                "count", guaranteeCount,
                "amount", guaranteeAmount
            ));
            monthData.put("claims", Map.of(
                "count", claimCount,
                "amount", claimAmount
            ));
            monthData.put("amendments", Map.of(
                "count", amendmentCount
            ));

            monthlyData.add(monthData);
            currentMonth = currentMonth.plusMonths(1);
        }

        return Map.of(
            "monthlyData", monthlyData,
            "period", Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "monthsBack", monthsBack
            )
        );
    }

    /**
     * Get metrics broken down by currency
     */
    public Map<String, Object> getMetricsByCurrency() {
        List<Object[]> guaranteeByCurrency = guaranteeRepository.getTotalAmountByCurrency();
        List<Object[]> claimsByCurrency = claimRepository.getTotalAmountByCurrency();

        Map<String, Object> result = new HashMap<>();
        result.put("guaranteesByCurrency", formatCurrencyData(guaranteeByCurrency));
        result.put("claimsByCurrency", formatCurrencyData(claimsByCurrency));

        return result;
    }

    /**
     * Get daily activity trend for the last N days
     */
    public Map<String, Object> getActivityTrend(int daysBack) {
        LocalDate startDate = LocalDate.now().minusDays(daysBack);
        LocalDate endDate = LocalDate.now();

        List<Object[]> dailyGuarantees = guaranteeRepository.getDailyActivityTrend(startDate, endDate);
        List<Object[]> dailyClaims = claimRepository.getDailyActivityTrend(startDate, endDate);
        List<Object[]> dailyAmendments = amendmentRepository.getDailyActivityTrend(startDate, endDate);

        return Map.of(
            "guarantees", formatDailyData(dailyGuarantees),
            "claims", formatDailyData(dailyClaims),
            "amendments", formatDailyData(dailyAmendments),
            "period", Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "daysBack", daysBack
            )
        );
    }

    /**
     * Helper method to format currency data
     */
    private List<Map<String, Object>> formatCurrencyData(List<Object[]> data) {
        return data.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("currency", (String) row[0]);
                map.put("amount", (BigDecimal) row[1]);
                map.put("count", (Long) row[2]);
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * Helper method to format daily data
     */
    private List<Map<String, Object>> formatDailyData(List<Object[]> data) {
        return data.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("date", row[0].toString());
                map.put("count", (Long) row[1]);
                return map;
            })
            .collect(Collectors.toList());
    }
}
