package com.interexport.guarantees.cqrs.query;

import com.interexport.guarantees.cqrs.event.GuaranteeCreatedEvent;
import com.interexport.guarantees.repository.query.GuaranteeSummaryViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Query Handler for Guarantee read operations
 * Handles events from Kafka and updates query database
 */
@Component
public class GuaranteeQueryHandler {
    
    private final GuaranteeSummaryViewRepository guaranteeSummaryViewRepository;
    
    @Autowired
    public GuaranteeQueryHandler(GuaranteeSummaryViewRepository guaranteeSummaryViewRepository) {
        this.guaranteeSummaryViewRepository = guaranteeSummaryViewRepository;
    }
    
    /**
     * Listen to guarantee created events and update query database
     */
    @KafkaListener(topics = "guarantee-created", groupId = "guarantees-cqrs-group")
    @Transactional
    public void handleGuaranteeCreated(GuaranteeCreatedEvent event) {
        try {
            // Create denormalized view for query side
            GuaranteeSummaryView view = new GuaranteeSummaryView();
            view.setGuaranteeId(event.getGuaranteeId());
            view.setReference(event.getReference());
            view.setGuaranteeType(event.getGuaranteeType());
            view.setAmount(event.getAmount());
            view.setCurrency(event.getCurrency());
            view.setIssueDate(event.getIssueDate());
            view.setExpiryDate(event.getExpiryDate());
            view.setBeneficiaryName(event.getBeneficiaryName());
            view.setStatus(event.getStatus());
            view.setCreatedAt(event.getCreatedAt());
            view.setUpdatedAt(LocalDateTime.now());
            
            // Calculate denormalized fields
            calculateDenormalizedFields(view);
            
            // Save to query database
            guaranteeSummaryViewRepository.save(view);
            
            System.out.println("Processed guarantee created event: " + event.getGuaranteeId());
            
        } catch (Exception e) {
            System.err.println("Error processing guarantee created event: " + e.getMessage());
            // In production, you might want to implement retry logic or dead letter queue
            throw e;
        }
    }
    
    /**
     * Get all guarantee summaries (optimized query)
     */
    public List<GuaranteeSummaryView> getAllGuaranteeSummaries() {
        return guaranteeSummaryViewRepository.findAll();
    }
    
    /**
     * Get guarantee summaries by status
     */
    public List<GuaranteeSummaryView> getGuaranteeSummariesByStatus(String status) {
        return guaranteeSummaryViewRepository.findByStatus(status);
    }
    
    /**
     * Get expiring guarantees (next 30 days)
     */
    public List<GuaranteeSummaryView> getExpiringGuarantees() {
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return guaranteeSummaryViewRepository.findByExpiryDateBetweenAndStatus(
            LocalDate.now(), thirtyDaysFromNow, "ACTIVE");
    }
    
    /**
     * Get guarantees by currency
     */
    public List<GuaranteeSummaryView> getGuaranteesByCurrency(String currency) {
        return guaranteeSummaryViewRepository.findByCurrency(currency);
    }
    
    /**
     * Calculate denormalized fields for better query performance
     */
    private void calculateDenormalizedFields(GuaranteeSummaryView view) {
        // Calculate days to expiry
        if (view.getExpiryDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), view.getExpiryDate());
            view.setDaysToExpiry((int) days);
        }
        
        // Set currency symbol
        switch (view.getCurrency()) {
            case "USD": view.setCurrencySymbol("$"); break;
            case "EUR": view.setCurrencySymbol("€"); break;
            case "GBP": view.setCurrencySymbol("£"); break;
            default: view.setCurrencySymbol(view.getCurrency()); break;
        }
        
        // Calculate risk level based on amount and days to expiry
        if (view.getAmount() != null && view.getDaysToExpiry() != null) {
            if (view.getAmount().compareTo(new java.math.BigDecimal("1000000")) > 0 || 
                view.getDaysToExpiry() < 30) {
                view.setRiskLevel("HIGH");
            } else if (view.getAmount().compareTo(new java.math.BigDecimal("500000")) > 0 || 
                      view.getDaysToExpiry() < 90) {
                view.setRiskLevel("MEDIUM");
            } else {
                view.setRiskLevel("LOW");
            }
        }
        
        // TODO: Calculate amount in USD using FX rates
        // This would require integration with FX rate service
        view.setAmountInUsd(view.getAmount());
    }
}
