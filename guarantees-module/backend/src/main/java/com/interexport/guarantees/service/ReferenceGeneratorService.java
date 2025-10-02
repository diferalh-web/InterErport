package com.interexport.guarantees.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for generating unique business references.
 */
@Service
public class ReferenceGeneratorService {

    private final AtomicLong guaranteeCounter = new AtomicLong(1);
    private final AtomicLong amendmentCounter = new AtomicLong(1);
    private final AtomicLong claimCounter = new AtomicLong(1);

    /**
     * Generate unique guarantee reference
     * Format: GT-YYYYMMDD-NNNNNN
     */
    public String generateGuaranteeReference() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%06d", guaranteeCounter.getAndIncrement());
        return "GT-" + dateStr + "-" + sequence;
    }

    /**
     * Generate unique amendment reference
     * Format: AM-YYYYMMDD-NNNNNN
     */
    public String generateAmendmentReference() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%06d", amendmentCounter.getAndIncrement());
        return "AM-" + dateStr + "-" + sequence;
    }

    /**
     * Generate unique claim reference
     * Format: CL-YYYYMMDD-NNNNNN
     */
    public String generateClaimReference() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%06d", claimCounter.getAndIncrement());
        return "CL-" + dateStr + "-" + sequence;
    }

    /**
     * Generate reference for specific guarantee
     * Format: GT-YYYYMMDD-NNNNNN-amendmentSequence
     */
    public String generateAmendmentReference(String guaranteeReference) {
        return guaranteeReference + "-A" + String.format("%02d", amendmentCounter.getAndIncrement() % 100);
    }

    /**
     * Generate claim reference for specific guarantee
     * Format: GT-YYYYMMDD-NNNNNN-claimSequence
     */
    public String generateClaimReference(String guaranteeReference) {
        return guaranteeReference + "-C" + String.format("%02d", claimCounter.getAndIncrement() % 100);
    }
}
