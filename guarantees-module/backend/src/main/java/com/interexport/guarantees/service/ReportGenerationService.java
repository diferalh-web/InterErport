package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import com.interexport.guarantees.repository.ClaimRepository;
import com.interexport.guarantees.repository.AmendmentRepository;
import com.interexport.guarantees.service.DashboardService;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.opencsv.CSVWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating reports in multiple formats (PDF, Excel, CSV)
 * Implements requirements from F9 - Reports (CSV/PDF/Excel)
 */
@Service
@Transactional(readOnly = true)
public class ReportGenerationService {

    private final GuaranteeContractRepository guaranteeRepository;
    private final ClaimRepository claimRepository;
    private final AmendmentRepository amendmentRepository;
    private final DashboardService dashboardService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public ReportGenerationService(GuaranteeContractRepository guaranteeRepository,
                                 ClaimRepository claimRepository,
                                 AmendmentRepository amendmentRepository,
                                 DashboardService dashboardService) {
        this.guaranteeRepository = guaranteeRepository;
        this.claimRepository = claimRepository;
        this.amendmentRepository = amendmentRepository;
        this.dashboardService = dashboardService;
    }

    /**
     * Generate Guarantees Report in PDF format
     * UC9.4: Download PDF with digital signature (optional)
     */
    public byte[] generateGuaranteesReportPDF(LocalDate fromDate, LocalDate toDate, String status) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Report Header
            document.add(new Paragraph("InterExport - Guarantees Report")
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
            
            document.add(new Paragraph(String.format("Report Period: %s to %s", 
                fromDate.format(DATE_FORMAT), toDate.format(DATE_FORMAT)))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph(String.format("Generated: %s", 
                LocalDateTime.now().format(DATETIME_FORMAT)))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT));

            // Get data
            List<GuaranteeContract> guarantees = getGuaranteesForReport(fromDate, toDate, status);

            // Summary Statistics
            document.add(new Paragraph("Summary Statistics").setFontSize(16).setBold());
            
            BigDecimal totalAmount = guarantees.stream()
                .map(GuaranteeContract::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Long> statusCounts = guarantees.stream()
                .collect(Collectors.groupingBy(g -> g.getStatus().name(), Collectors.counting()));
            
            Table summaryTable = new Table(UnitValue.createPercentArray(2));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Guarantees")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(guarantees.size()))));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Amount")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("$%,.2f", totalAmount))));
            
            for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
                summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(entry.getKey() + " Status")).setBold());
                summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(entry.getValue()))));
            }
            
            document.add(summaryTable);

            // Detailed Table
            document.add(new Paragraph("Guarantee Details").setFontSize(16).setBold());
            
            Table detailTable = new Table(UnitValue.createPercentArray(7));
            detailTable.setWidth(UnitValue.createPercentValue(100));
            
            // Header
            String[] headers = {"Reference", "Type", "Amount", "Currency", "Status", "Issue Date", "Expiry Date"};
            for (String header : headers) {
                detailTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)).setBold());
            }
            
            // Data rows
            for (GuaranteeContract guarantee : guarantees) {
                detailTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(guarantee.getReference())));
                detailTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(guarantee.getGuaranteeType().name())));
                detailTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%,.2f", guarantee.getAmount()))));
                detailTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(guarantee.getCurrency())));
                detailTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(guarantee.getStatus().name())));
                detailTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                    guarantee.getIssueDate() != null ? guarantee.getIssueDate().format(DATE_FORMAT) : "N/A")));
                detailTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                    guarantee.getExpiryDate() != null ? guarantee.getExpiryDate().format(DATE_FORMAT) : "N/A")));
            }
            
            document.add(detailTable);

            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    /**
     * Generate Guarantees Report in Excel format
     */
    public byte[] generateGuaranteesReportExcel(LocalDate fromDate, LocalDate toDate, String status) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Guarantees Report");
            
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

            // Report Header
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("InterExport - Guarantees Report");
            titleCell.setCellStyle(headerStyle);

            Row periodRow = sheet.createRow(1);
            periodRow.createCell(0).setCellValue(String.format("Report Period: %s to %s", 
                fromDate.format(DATE_FORMAT), toDate.format(DATE_FORMAT)));

            // Get data
            List<GuaranteeContract> guarantees = getGuaranteesForReport(fromDate, toDate, status);

            // Data headers
            Row headerRow = sheet.createRow(3);
            String[] headers = {"Reference", "Type", "Amount", "Currency", "Status", 
                              "Applicant ID", "Beneficiary", "Issue Date", "Expiry Date", "Domestic"};
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 4;
            for (GuaranteeContract guarantee : guarantees) {
                Row dataRow = sheet.createRow(rowNum++);
                
                dataRow.createCell(0).setCellValue(guarantee.getReference());
                dataRow.createCell(1).setCellValue(guarantee.getGuaranteeType().name());
                
                org.apache.poi.ss.usermodel.Cell amountCell = dataRow.createCell(2);
                amountCell.setCellValue(guarantee.getAmount().doubleValue());
                amountCell.setCellStyle(currencyStyle);
                
                dataRow.createCell(3).setCellValue(guarantee.getCurrency());
                dataRow.createCell(4).setCellValue(guarantee.getStatus().name());
                dataRow.createCell(5).setCellValue(guarantee.getApplicantId());
                dataRow.createCell(6).setCellValue(guarantee.getBeneficiaryName());
                
                if (guarantee.getIssueDate() != null) {
                    org.apache.poi.ss.usermodel.Cell dateCell = dataRow.createCell(7);
                    dateCell.setCellValue(guarantee.getIssueDate().toString());
                    dateCell.setCellStyle(dateStyle);
                }
                
                if (guarantee.getExpiryDate() != null) {
                    org.apache.poi.ss.usermodel.Cell dateCell = dataRow.createCell(8);
                    dateCell.setCellValue(guarantee.getExpiryDate().toString());
                    dateCell.setCellStyle(dateStyle);
                }
                
                dataRow.createCell(9).setCellValue(guarantee.getIsDomestic() ? "Yes" : "No");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    /**
     * Generate Guarantees Report in CSV format
     * UC9.1: Report of active transactions in CSV
     * UC9.5: Exact UTF-8 CSV export
     */
    public String generateGuaranteesReportCSV(LocalDate fromDate, LocalDate toDate, String status) {
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            
            // Get data
            List<GuaranteeContract> guarantees = getGuaranteesForReport(fromDate, toDate, status);
            
            // Header
            String[] headers = {"Reference", "Type", "Amount", "Currency", "Status", 
                              "Applicant ID", "Beneficiary", "Issue Date", "Expiry Date", 
                              "Domestic", "Created Date"};
            csvWriter.writeNext(headers);

            // Data rows
            for (GuaranteeContract guarantee : guarantees) {
                String[] row = {
                    guarantee.getReference(),
                    guarantee.getGuaranteeType().name(),
                    guarantee.getAmount().toString(),
                    guarantee.getCurrency(),
                    guarantee.getStatus().name(),
                    guarantee.getApplicantId().toString(),
                    guarantee.getBeneficiaryName(),
                    guarantee.getIssueDate() != null ? guarantee.getIssueDate().format(DATE_FORMAT) : "",
                    guarantee.getExpiryDate() != null ? guarantee.getExpiryDate().format(DATE_FORMAT) : "",
                    guarantee.getIsDomestic() ? "Yes" : "No",
                    guarantee.getCreatedDate().format(DATETIME_FORMAT)
                };
                csvWriter.writeNext(row);
            }
            
            return stringWriter.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }

    /**
     * Generate Dashboard Summary Report in PDF
     */
    public byte[] generateDashboardSummaryReportPDF(int monthsBack) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Report Header
            document.add(new Paragraph("InterExport - Dashboard Summary Report")
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
            
            document.add(new Paragraph(String.format("Report Period: Last %d months", monthsBack))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph(String.format("Generated: %s", 
                LocalDateTime.now().format(DATETIME_FORMAT)))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT));

            // Get dashboard data
            Map<String, Object> summaryData = dashboardService.getDashboardSummary();
            Map<String, Object> currencyData = dashboardService.getMetricsByCurrency();

            // Summary Statistics
            document.add(new Paragraph("Overall Statistics").setFontSize(16).setBold());
            
            Table summaryTable = new Table(UnitValue.createPercentArray(3));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Metric")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Count")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Amount")).setBold());
            
            // Guarantees
            @SuppressWarnings("unchecked")
            Map<String, Object> guaranteeStats = (Map<String, Object>) summaryData.get("guarantees");
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Guarantees")));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(guaranteeStats.get("total").toString())));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                String.format("$%,.2f", (BigDecimal) guaranteeStats.get("totalAmount")))));
            
            // Claims
            @SuppressWarnings("unchecked")
            Map<String, Object> claimStats = (Map<String, Object>) summaryData.get("claims");
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Claims")));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(claimStats.get("total").toString())));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                String.format("$%,.2f", (BigDecimal) claimStats.get("totalAmount")))));
            
            // Amendments
            @SuppressWarnings("unchecked")
            Map<String, Object> amendmentStats = (Map<String, Object>) summaryData.get("amendments");
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Amendments")));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(amendmentStats.get("total").toString())));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("N/A")));
            
            document.add(summaryTable);

            // Currency Breakdown
            document.add(new Paragraph("Currency Breakdown").setFontSize(16).setBold());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> guaranteesByCurrency = 
                (List<Map<String, Object>>) currencyData.get("guaranteesByCurrency");
            
            Table currencyTable = new Table(UnitValue.createPercentArray(3));
            currencyTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Currency")).setBold());
            currencyTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Count")).setBold());
            currencyTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Amount")).setBold());
            
            for (Map<String, Object> currencyStats : guaranteesByCurrency) {
                currencyTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(currencyStats.get("currency").toString())));
                currencyTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(currencyStats.get("count").toString())));
                currencyTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                    String.format("$%,.2f", (BigDecimal) currencyStats.get("amount")))));
            }
            
            document.add(currencyTable);

            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dashboard PDF report", e);
        }
    }

    /**
     * Generate Commission Report in Excel format
     * UC9.2: Commission report by date range
     */
    public byte[] generateCommissionReportExcel(LocalDate fromDate, LocalDate toDate) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Commission Report");
            
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            CellStyle currencyStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

            // Report Header
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("InterExport - Commission Report");
            titleCell.setCellStyle(headerStyle);

            Row periodRow = sheet.createRow(1);
            periodRow.createCell(0).setCellValue(String.format("Report Period: %s to %s", 
                fromDate.format(DATE_FORMAT), toDate.format(DATE_FORMAT)));

            // This would integrate with the commission calculation service
            // For now, create a placeholder structure
            Row headerRow = sheet.createRow(3);
            String[] headers = {"Guarantee Reference", "Commission Type", "Amount", "Currency", 
                              "Due Date", "Status", "Installment"};
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate commission Excel report", e);
        }
    }

    /**
     * Helper method to get guarantees for reporting based on criteria
     */
    private List<GuaranteeContract> getGuaranteesForReport(LocalDate fromDate, LocalDate toDate, String status) {
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            // Filter by status if specified
            return guaranteeRepository.findByCreatedDateBetweenAndStatusOrderByCreatedDateDesc(
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay(), 
                com.interexport.guarantees.entity.enums.GuaranteeStatus.valueOf(status.toUpperCase()));
        } else {
            // All guarantees in date range
            return guaranteeRepository.findByCreatedDateBetweenOrderByCreatedDateDesc(
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());
        }
    }

    /**
     * Generate report filename based on type and parameters
     */
    public String generateReportFilename(String reportType, String format, LocalDate fromDate, LocalDate toDate) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dateRange = String.format("%s_to_%s", 
            fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        return String.format("%s_Report_%s_%s.%s", 
            reportType, dateRange, timestamp, format.toLowerCase());
    }

    /**
     * Generate Commission Report in PDF format
     */
    public byte[] generateCommissionReportPDF(LocalDate fromDate, LocalDate toDate, boolean includeProjections) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Report Header
            document.add(new Paragraph("InterExport - Commission Report")
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
            
            document.add(new Paragraph(String.format("Report Period: %s to %s", 
                fromDate.format(DATE_FORMAT), toDate.format(DATE_FORMAT)))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph(String.format("Generated: %s", 
                LocalDateTime.now().format(DATETIME_FORMAT)))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT));

            // Commission summary
            document.add(new Paragraph("Commission Summary").setFontSize(16).setBold());
            
            // Get commission data (simplified for now)
            BigDecimal totalCommissions = BigDecimal.ZERO; // TODO: Calculate from actual data
            
            Table summaryTable = new Table(UnitValue.createPercentArray(2));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Commissions")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(totalCommissions.toString())));
            
            document.add(summaryTable);
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate commission PDF report", e);
        }
    }

    /**
     * Generate Commission Report in Excel format
     */
    public byte[] generateCommissionReportExcel(LocalDate fromDate, LocalDate toDate, boolean includeProjections) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Commission Report");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Date", "Guarantee Reference", "Commission Amount", "Currency", "Status"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            // Add sample data (TODO: Replace with actual data)
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(fromDate.format(DATE_FORMAT));
            dataRow.createCell(1).setCellValue("GT-20250101-001");
            dataRow.createCell(2).setCellValue(1000.00);
            dataRow.createCell(3).setCellValue("USD");
            dataRow.createCell(4).setCellValue("PAID");

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate commission Excel report", e);
        }
    }

    /**
     * Generate Commission Report in CSV format
     */
    public String generateCommissionReportCSV(LocalDate fromDate, LocalDate toDate, boolean includeProjections) {
        try (StringWriter writer = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            // Write header
            csvWriter.writeNext(new String[]{"Date", "Guarantee Reference", "Commission Amount", "Currency", "Status"});
            
            // Write sample data (TODO: Replace with actual data)
            csvWriter.writeNext(new String[]{
                fromDate.format(DATE_FORMAT),
                "GT-20250101-001",
                "1000.00",
                "USD",
                "PAID"
            });
            
            return writer.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate commission CSV report", e);
        }
    }

    /**
     * Generate Active Transactions Report in PDF format
     */
    public byte[] generateActiveTransactionsReportPDF(LocalDate fromDate, LocalDate toDate, boolean includeDrafts, boolean includeExpired) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Report Header
            document.add(new Paragraph("InterExport - Active Transactions Report")
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
            
            document.add(new Paragraph(String.format("Report Period: %s to %s", 
                fromDate.format(DATE_FORMAT), toDate.format(DATE_FORMAT)))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));

            // Get active transactions data
            List<GuaranteeContract> guarantees = getGuaranteesForReport(fromDate, toDate, "ALL");
            
            // Filter based on parameters
            if (!includeDrafts) {
                guarantees = guarantees.stream()
                    .filter(g -> !g.getStatus().name().equals("DRAFT"))
                    .collect(Collectors.toList());
            }
            
            if (!includeExpired) {
                LocalDate now = LocalDate.now();
                guarantees = guarantees.stream()
                    .filter(g -> g.getExpiryDate().isAfter(now))
                    .collect(Collectors.toList());
            }

            // Summary
            document.add(new Paragraph("Active Transactions Summary").setFontSize(16).setBold());
            
            Table summaryTable = new Table(UnitValue.createPercentArray(2));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Active Guarantees")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(guarantees.size()))));
            
            BigDecimal totalAmount = guarantees.stream()
                .map(GuaranteeContract::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Amount")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(totalAmount.toString())));
            
            document.add(summaryTable);
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate active transactions PDF report", e);
        }
    }

    /**
     * Generate Active Transactions Report in Excel format
     */
    public byte[] generateActiveTransactionsReportExcel(LocalDate fromDate, LocalDate toDate, boolean includeDrafts, boolean includeExpired) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Active Transactions");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Reference", "Type", "Amount", "Currency", "Status", "Issue Date", "Expiry Date"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            // Get and add data
            List<GuaranteeContract> guarantees = getGuaranteesForReport(fromDate, toDate, "ALL");
            
            int rowNum = 1;
            for (GuaranteeContract guarantee : guarantees) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(guarantee.getReference());
                row.createCell(1).setCellValue(guarantee.getGuaranteeType().name());
                row.createCell(2).setCellValue(guarantee.getAmount().doubleValue());
                row.createCell(3).setCellValue(guarantee.getCurrency());
                row.createCell(4).setCellValue(guarantee.getStatus().name());
                row.createCell(5).setCellValue(guarantee.getIssueDate().format(DATE_FORMAT));
                row.createCell(6).setCellValue(guarantee.getExpiryDate().format(DATE_FORMAT));
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate active transactions Excel report", e);
        }
    }

    /**
     * Generate Active Transactions Report in CSV format
     */
    public String generateActiveTransactionsReportCSV(LocalDate fromDate, LocalDate toDate, boolean includeDrafts, boolean includeExpired) {
        try (StringWriter writer = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            // Write header
            csvWriter.writeNext(new String[]{"Reference", "Type", "Amount", "Currency", "Status", "Issue Date", "Expiry Date"});
            
            // Get and write data
            List<GuaranteeContract> guarantees = getGuaranteesForReport(fromDate, toDate, "ALL");
            
            for (GuaranteeContract guarantee : guarantees) {
                csvWriter.writeNext(new String[]{
                    guarantee.getReference(),
                    guarantee.getGuaranteeType().name(),
                    guarantee.getAmount().toString(),
                    guarantee.getCurrency(),
                    guarantee.getStatus().name(),
                    guarantee.getIssueDate().format(DATE_FORMAT),
                    guarantee.getExpiryDate().format(DATE_FORMAT)
                });
            }
            
            return writer.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate active transactions CSV report", e);
        }
    }

    /**
     * Generate Audit Report in PDF format
     */
    public byte[] generateAuditReportPDF(LocalDate fromDate, LocalDate toDate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Report Header
            document.add(new Paragraph("InterExport - Audit Report")
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
            
            document.add(new Paragraph(String.format("Report Period: %s to %s", 
                fromDate.format(DATE_FORMAT), toDate.format(DATE_FORMAT)))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));

            // Audit summary
            document.add(new Paragraph("Audit Summary").setFontSize(16).setBold());
            
            Table summaryTable = new Table(UnitValue.createPercentArray(2));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Audit Period")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                String.format("%s to %s", fromDate.format(DATE_FORMAT), toDate.format(DATE_FORMAT)))));
            
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Generated")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                LocalDateTime.now().format(DATETIME_FORMAT))));
            
            document.add(summaryTable);
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate audit PDF report", e);
        }
    }

    /**
     * Generate Audit Report in Excel format
     */
    public byte[] generateAuditReportExcel(LocalDate fromDate, LocalDate toDate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Audit Report");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Date", "Action", "User", "Entity", "Details"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            // Add sample audit data (TODO: Replace with actual audit data)
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(fromDate.format(DATE_FORMAT));
            dataRow.createCell(1).setCellValue("CREATE");
            dataRow.createCell(2).setCellValue("admin");
            dataRow.createCell(3).setCellValue("Guarantee");
            dataRow.createCell(4).setCellValue("Guarantee created successfully");

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate audit Excel report", e);
        }
    }

    /**
     * Generate Audit Report in CSV format
     */
    public String generateAuditReportCSV(LocalDate fromDate, LocalDate toDate) {
        try (StringWriter writer = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            // Write header
            csvWriter.writeNext(new String[]{"Date", "Action", "User", "Entity", "Details"});
            
            // Write sample audit data (TODO: Replace with actual audit data)
            csvWriter.writeNext(new String[]{
                fromDate.format(DATE_FORMAT),
                "CREATE",
                "admin",
                "Guarantee",
                "Guarantee created successfully"
            });
            
            return writer.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate audit CSV report", e);
        }
    }

    /**
     * Generate Expiry Alert Report in PDF format
     */
    public byte[] generateExpiryAlertReportPDF(int daysAhead) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Report Header
            document.add(new Paragraph("InterExport - Expiry Alert Report")
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
            
            document.add(new Paragraph(String.format("Guarantees expiring within %d days", daysAhead))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));

            // Get expiring guarantees
            LocalDate alertDate = LocalDate.now().plusDays(daysAhead);
            List<GuaranteeContract> expiringGuarantees = guaranteeRepository.findByExpiryDateBetweenOrderByExpiryDateAsc(
                LocalDate.now(), alertDate);

            // Summary
            document.add(new Paragraph("Expiry Alert Summary").setFontSize(16).setBold());
            
            Table summaryTable = new Table(UnitValue.createPercentArray(2));
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Guarantees Expiring")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(expiringGuarantees.size()))));
            
            BigDecimal totalAmount = expiringGuarantees.stream()
                .map(GuaranteeContract::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Total Amount")).setBold());
            summaryTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(totalAmount.toString())));
            
            document.add(summaryTable);
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate expiry alert PDF report", e);
        }
    }

    /**
     * Generate Expiry Alert Report in Excel format
     */
    public byte[] generateExpiryAlertReportExcel(int daysAhead) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Expiry Alert");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Reference", "Type", "Amount", "Currency", "Expiry Date", "Days Until Expiry"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            // Get and add expiring guarantees
            LocalDate alertDate = LocalDate.now().plusDays(daysAhead);
            List<GuaranteeContract> expiringGuarantees = guaranteeRepository.findByExpiryDateBetweenOrderByExpiryDateAsc(
                LocalDate.now(), alertDate);
            
            int rowNum = 1;
            for (GuaranteeContract guarantee : expiringGuarantees) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(guarantee.getReference());
                row.createCell(1).setCellValue(guarantee.getGuaranteeType().name());
                row.createCell(2).setCellValue(guarantee.getAmount().doubleValue());
                row.createCell(3).setCellValue(guarantee.getCurrency());
                row.createCell(4).setCellValue(guarantee.getExpiryDate().format(DATE_FORMAT));
                row.createCell(5).setCellValue(LocalDate.now().until(guarantee.getExpiryDate()).getDays());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate expiry alert Excel report", e);
        }
    }

    /**
     * Generate Expiry Alert Report in CSV format
     */
    public String generateExpiryAlertReportCSV(int daysAhead) {
        try (StringWriter writer = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            // Write header
            csvWriter.writeNext(new String[]{"Reference", "Type", "Amount", "Currency", "Expiry Date", "Days Until Expiry"});
            
            // Get and write expiring guarantees
            LocalDate alertDate = LocalDate.now().plusDays(daysAhead);
            List<GuaranteeContract> expiringGuarantees = guaranteeRepository.findByExpiryDateBetweenOrderByExpiryDateAsc(
                LocalDate.now(), alertDate);
            
            for (GuaranteeContract guarantee : expiringGuarantees) {
                csvWriter.writeNext(new String[]{
                    guarantee.getReference(),
                    guarantee.getGuaranteeType().name(),
                    guarantee.getAmount().toString(),
                    guarantee.getCurrency(),
                    guarantee.getExpiryDate().format(DATE_FORMAT),
                    String.valueOf(LocalDate.now().until(guarantee.getExpiryDate()).getDays())
                });
            }
            
            return writer.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate expiry alert CSV report", e);
        }
    }

    /**
     * Create header style for Excel cells
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Get content type for different report formats
     */
    public String getContentType(String format) {
        switch (format.toUpperCase()) {
            case "PDF":
                return "application/pdf";
            case "EXCEL":
            case "XLSX":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "CSV":
                return "text/csv";
            default:
                return "application/octet-stream";
        }
    }
}
