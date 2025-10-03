package com.interexport.guarantees.controller;

import com.interexport.guarantees.service.ReportGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for report generation endpoints
 * Implements F9 - Reports (CSV/PDF/Excel)
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Report Generation", description = "API for generating reports in various formats")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('USER')")
public class ReportController {

    private final ReportGenerationService reportGenerationService;

    @Autowired
    public ReportController(ReportGenerationService reportGenerationService) {
        this.reportGenerationService = reportGenerationService;
    }

    /**
     * Generate Guarantees Report in PDF format
     */
    @GetMapping("/guarantees/pdf")
    @Operation(summary = "Generate Guarantees Report as PDF",
               description = "Generate a comprehensive PDF report of guarantees for the specified date range")
    public ResponseEntity<byte[]> generateGuaranteesReportPDF(
            @Parameter(description = "Start date for report (YYYY-MM-DD)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            
            @Parameter(description = "End date for report (YYYY-MM-DD)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            
            @Parameter(description = "Filter by status (DRAFT, APPROVED, etc.) or ALL", example = "ALL")
            @RequestParam(defaultValue = "ALL") String status) {

        byte[] pdfContent = reportGenerationService.generateGuaranteesReportPDF(fromDate, toDate, status);
        String filename = reportGenerationService.generateReportFilename("Guarantees", "PDF", fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    /**
     * Generate Guarantees Report in Excel format
     */
    @GetMapping("/guarantees/excel")
    @Operation(summary = "Generate Guarantees Report as Excel",
               description = "Generate a comprehensive Excel report of guarantees for the specified date range")
    public ResponseEntity<byte[]> generateGuaranteesReportExcel(
            @Parameter(description = "Start date for report (YYYY-MM-DD)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            
            @Parameter(description = "End date for report (YYYY-MM-DD)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            
            @Parameter(description = "Filter by status (DRAFT, APPROVED, etc.) or ALL", example = "ALL")
            @RequestParam(defaultValue = "ALL") String status) {

        byte[] excelContent = reportGenerationService.generateGuaranteesReportExcel(fromDate, toDate, status);
        String filename = reportGenerationService.generateReportFilename("Guarantees", "XLSX", fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(reportGenerationService.getContentType("EXCEL")))
                .body(excelContent);
    }

    /**
     * Generate Guarantees Report in CSV format
     */
    @GetMapping("/guarantees/csv")
    @Operation(summary = "Generate Guarantees Report as CSV", 
               description = "Generate a CSV report of guarantees for the specified date range")
    public ResponseEntity<String> generateGuaranteesReportCSV(
            @Parameter(description = "Start date for report (YYYY-MM-DD)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            
            @Parameter(description = "End date for report (YYYY-MM-DD)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            
            @Parameter(description = "Filter by status (DRAFT, APPROVED, etc.) or ALL", example = "ALL")
            @RequestParam(defaultValue = "ALL") String status) {

        String csvContent = reportGenerationService.generateGuaranteesReportCSV(fromDate, toDate, status);
        String filename = reportGenerationService.generateReportFilename("Guarantees", "CSV", fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvContent);
    }

    /**
     * Generate Dashboard Summary Report in PDF format
     */
    @GetMapping("/dashboard/pdf")
    @Operation(summary = "Generate Dashboard Summary Report as PDF",
               description = "Generate a PDF report with dashboard statistics and analytics")
    public ResponseEntity<byte[]> generateDashboardSummaryReportPDF(
            @Parameter(description = "Number of months to include in report", example = "12")
            @RequestParam(defaultValue = "12") int monthsBack) {

        byte[] pdfContent = reportGenerationService.generateDashboardSummaryReportPDF(monthsBack);
        String filename = String.format("Dashboard_Summary_%d_months_%s.pdf", 
            monthsBack, java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    /**
     * Generate Commission Report in Excel format
     */
    @GetMapping("/commissions/excel")
    @Operation(summary = "Generate Commission Report as Excel",
               description = "Generate an Excel report of commission calculations for the specified date range")
    public ResponseEntity<byte[]> generateCommissionReportExcel(
            @Parameter(description = "Start date for report (YYYY-MM-DD)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            
            @Parameter(description = "End date for report (YYYY-MM-DD)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        byte[] excelContent = reportGenerationService.generateCommissionReportExcel(fromDate, toDate);
        String filename = reportGenerationService.generateReportFilename("Commission", "XLSX", fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(reportGenerationService.getContentType("EXCEL")))
                .body(excelContent);
    }

    /**
     * Generate Commission Report
     */
    @GetMapping("/commissions")
    @Operation(summary = "Generate Commission Report",
               description = "Generate a commission report for the specified date range")
    public ResponseEntity<byte[]> generateCommissionReport(
            @Parameter(description = "Start date for report (YYYY-MM-DD)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for report (YYYY-MM-DD)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Report format (PDF, EXCEL, CSV)", example = "PDF")
            @RequestParam(defaultValue = "PDF") String format,
            
            @Parameter(description = "Include projections", example = "false")
            @RequestParam(defaultValue = "false") boolean includeProjections) {

        byte[] content;
        String filename;
        MediaType contentType;

        switch (format.toUpperCase()) {
            case "PDF":
                content = reportGenerationService.generateCommissionReportPDF(startDate, endDate, includeProjections);
                filename = reportGenerationService.generateReportFilename("Commission", "PDF", startDate, endDate);
                contentType = MediaType.APPLICATION_PDF;
                break;
            case "EXCEL":
                content = reportGenerationService.generateCommissionReportExcel(startDate, endDate, includeProjections);
                filename = reportGenerationService.generateReportFilename("Commission", "XLSX", startDate, endDate);
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case "CSV":
                String csvContent = reportGenerationService.generateCommissionReportCSV(startDate, endDate, includeProjections);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + 
                                reportGenerationService.generateReportFilename("Commission", "CSV", startDate, endDate) + "\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(csvContent.getBytes());
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(content);
    }

    /**
     * Generate Active Transactions Report
     */
    @GetMapping("/active-transactions")
    @Operation(summary = "Generate Active Transactions Report",
               description = "Generate an active transactions report for the specified date range")
    public ResponseEntity<byte[]> generateActiveTransactionsReport(
            @Parameter(description = "Start date for report (YYYY-MM-DD)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for report (YYYY-MM-DD)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Report format (PDF, EXCEL, CSV)", example = "PDF")
            @RequestParam(defaultValue = "PDF") String format,
            
            @Parameter(description = "Include drafts", example = "false")
            @RequestParam(defaultValue = "false") boolean includeDrafts,
            
            @Parameter(description = "Include expired", example = "false")
            @RequestParam(defaultValue = "false") boolean includeExpired) {

        byte[] content;
        String filename;
        MediaType contentType;

        switch (format.toUpperCase()) {
            case "PDF":
                content = reportGenerationService.generateActiveTransactionsReportPDF(startDate, endDate, includeDrafts, includeExpired);
                filename = reportGenerationService.generateReportFilename("ActiveTransactions", "PDF", startDate, endDate);
                contentType = MediaType.APPLICATION_PDF;
                break;
            case "EXCEL":
                content = reportGenerationService.generateActiveTransactionsReportExcel(startDate, endDate, includeDrafts, includeExpired);
                filename = reportGenerationService.generateReportFilename("ActiveTransactions", "XLSX", startDate, endDate);
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case "CSV":
                String csvContent = reportGenerationService.generateActiveTransactionsReportCSV(startDate, endDate, includeDrafts, includeExpired);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + 
                                reportGenerationService.generateReportFilename("ActiveTransactions", "CSV", startDate, endDate) + "\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(csvContent.getBytes());
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(content);
    }

    /**
     * Generate Audit Report
     */
    @GetMapping("/audit")
    @Operation(summary = "Generate Audit Report",
               description = "Generate an audit report for the specified date range")
    public ResponseEntity<byte[]> generateAuditReport(
            @Parameter(description = "Start date for report (YYYY-MM-DD)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date for report (YYYY-MM-DD)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Report format (PDF, EXCEL, CSV)", example = "PDF")
            @RequestParam(defaultValue = "PDF") String format) {

        byte[] content;
        String filename;
        MediaType contentType;

        switch (format.toUpperCase()) {
            case "PDF":
                content = reportGenerationService.generateAuditReportPDF(startDate, endDate);
                filename = reportGenerationService.generateReportFilename("Audit", "PDF", startDate, endDate);
                contentType = MediaType.APPLICATION_PDF;
                break;
            case "EXCEL":
                content = reportGenerationService.generateAuditReportExcel(startDate, endDate);
                filename = reportGenerationService.generateReportFilename("Audit", "XLSX", startDate, endDate);
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case "CSV":
                String csvContent = reportGenerationService.generateAuditReportCSV(startDate, endDate);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + 
                                reportGenerationService.generateReportFilename("Audit", "CSV", startDate, endDate) + "\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(csvContent.getBytes());
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(content);
    }

    /**
     * Generate Expiry Alert Report
     */
    @GetMapping("/expiry-alerts")
    @Operation(summary = "Generate Expiry Alert Report",
               description = "Generate an expiry alert report for guarantees expiring within the specified days")
    public ResponseEntity<byte[]> generateExpiryAlertReport(
            @Parameter(description = "Days ahead to check for expiry", example = "30")
            @RequestParam(defaultValue = "30") int daysAhead,
            
            @Parameter(description = "Report format (PDF, EXCEL, CSV)", example = "PDF")
            @RequestParam(defaultValue = "PDF") String format) {

        byte[] content;
        String filename;
        MediaType contentType;

        switch (format.toUpperCase()) {
            case "PDF":
                content = reportGenerationService.generateExpiryAlertReportPDF(daysAhead);
                filename = reportGenerationService.generateReportFilename("ExpiryAlert", "PDF", 
                        LocalDate.now(), LocalDate.now().plusDays(daysAhead));
                contentType = MediaType.APPLICATION_PDF;
                break;
            case "EXCEL":
                content = reportGenerationService.generateExpiryAlertReportExcel(daysAhead);
                filename = reportGenerationService.generateReportFilename("ExpiryAlert", "XLSX", 
                        LocalDate.now(), LocalDate.now().plusDays(daysAhead));
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case "CSV":
                String csvContent = reportGenerationService.generateExpiryAlertReportCSV(daysAhead);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + 
                                reportGenerationService.generateReportFilename("ExpiryAlert", "CSV", 
                                        LocalDate.now(), LocalDate.now().plusDays(daysAhead)) + "\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(csvContent.getBytes());
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(content);
    }

    /**
     * Get report generation status and available report types
     */
    @GetMapping("/status")
    @Operation(summary = "Get Report Generation Status",
               description = "Get information about available report types and generation status")
    public ResponseEntity<ReportStatusResponse> getReportStatus() {
        ReportStatusResponse status = new ReportStatusResponse();
        status.setAvailableFormats(java.util.Arrays.asList("PDF", "EXCEL", "CSV"));
        status.setAvailableReports(java.util.Arrays.asList(
            "Guarantees Report", 
            "Dashboard Summary Report", 
            "Commission Report",
            "Active Transactions Report",
            "Audit Report",
            "Expiry Alert Report"
        ));
        status.setReportGenerationEnabled(true);
        status.setMaxDateRange(365); // Maximum 365 days
        
        return ResponseEntity.ok(status);
    }

    /**
     * Response class for report status endpoint
     */
    public static class ReportStatusResponse {
        private java.util.List<String> availableFormats;
        private java.util.List<String> availableReports;
        private boolean reportGenerationEnabled;
        private int maxDateRange;

        // Getters and Setters
        public java.util.List<String> getAvailableFormats() { return availableFormats; }
        public void setAvailableFormats(java.util.List<String> availableFormats) { this.availableFormats = availableFormats; }

        public java.util.List<String> getAvailableReports() { return availableReports; }
        public void setAvailableReports(java.util.List<String> availableReports) { this.availableReports = availableReports; }

        public boolean isReportGenerationEnabled() { return reportGenerationEnabled; }
        public void setReportGenerationEnabled(boolean reportGenerationEnabled) { this.reportGenerationEnabled = reportGenerationEnabled; }

        public int getMaxDateRange() { return maxDateRange; }
        public void setMaxDateRange(int maxDateRange) { this.maxDateRange = maxDateRange; }
    }
}