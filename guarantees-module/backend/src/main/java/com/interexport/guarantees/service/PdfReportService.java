package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.FeeItem;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import com.interexport.guarantees.repository.FeeItemRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating PDF reports using iText 7
 * Implements F9 - Reports functionality with actual PDF generation
 */
@Service
public class PdfReportService {

    private final GuaranteeContractRepository guaranteeRepository;
    private final FeeItemRepository feeItemRepository;

    @Autowired
    public PdfReportService(GuaranteeContractRepository guaranteeRepository,
                           FeeItemRepository feeItemRepository) {
        this.guaranteeRepository = guaranteeRepository;
        this.feeItemRepository = feeItemRepository;
    }

    /**
     * Generate Active Transactions Report PDF
     */
    public byte[] generateActiveTransactionsReport(LocalDate startDate, LocalDate endDate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            Paragraph title = new Paragraph("Active Transactions Report")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Date range
            Paragraph dateRange = new Paragraph(String.format("Report Period: %s to %s", 
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(dateRange);
            document.add(new Paragraph("\n"));

            // Get active guarantees
            List<GuaranteeContract> activeGuarantees = guaranteeRepository.findAll().stream()
                    .filter(g -> g.getStatus().name().equals("APPROVED") || g.getStatus().name().equals("SUBMITTED"))
                    .collect(Collectors.toList());

            // Create table
            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 25, 15, 15, 15, 15}))
                    .useAllAvailableWidth();

            // Headers
            table.addHeaderCell(new Cell().add(new Paragraph("Reference").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Applicant").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Currency").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Expiry Date").setBold()));

            // Data rows
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (GuaranteeContract guarantee : activeGuarantees) {
                table.addCell(guarantee.getReference());
                table.addCell(guarantee.getApplicantId() != null ? "Client-" + guarantee.getApplicantId() : "N/A");
                table.addCell(String.format("%.2f", guarantee.getAmount()));
                table.addCell(guarantee.getCurrency());
                table.addCell(guarantee.getStatus().toString());
                table.addCell(guarantee.getExpiryDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                totalAmount = totalAmount.add(guarantee.getAmount());
            }

            document.add(table);

            // Summary
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Summary:")
                    .setFontSize(14)
                    .setBold());
            document.add(new Paragraph(String.format("Total Active Guarantees: %d", activeGuarantees.size())));
            document.add(new Paragraph(String.format("Total Amount: %.2f", totalAmount)));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    /**
     * Generate Commission Report PDF
     */
    public byte[] generateCommissionReport(LocalDate startDate, LocalDate endDate, boolean includeProjections) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            Paragraph title = new Paragraph("Commission Report")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Date range
            Paragraph dateRange = new Paragraph(String.format("Report Period: %s to %s", 
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(dateRange);
            document.add(new Paragraph("\n"));

            // Get commission data
            List<FeeItem> commissions = feeItemRepository.findAll().stream()
                    .filter(f -> f.getFeeType().toString().contains("COMMISSION"))
                    .collect(Collectors.toList());

            // Create table
            Table table = new Table(UnitValue.createPercentArray(new float[]{25, 15, 15, 15, 15, 15}))
                    .useAllAvailableWidth();

            // Headers
            table.addHeaderCell(new Cell().add(new Paragraph("Guarantee Reference").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Commission Type").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Currency").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Rate %").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Due Date").setBold()));

            // Data rows
            BigDecimal totalCommissions = BigDecimal.ZERO;
            for (FeeItem commission : commissions) {
                table.addCell(commission.getGuarantee().getReference());
                table.addCell(commission.getFeeType().toString());
                table.addCell(String.format("%.2f", commission.getAmount()));
                table.addCell(commission.getCurrency());
                table.addCell(String.format("%.2f", commission.getRate()));
                table.addCell(commission.getDueDate() != null ? 
                    commission.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A");
                totalCommissions = totalCommissions.add(commission.getAmount());
            }

            document.add(table);

            // Summary
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Summary:")
                    .setFontSize(14)
                    .setBold());
            document.add(new Paragraph(String.format("Total Commission Items: %d", commissions.size())));
            document.add(new Paragraph(String.format("Total Commission Amount: %.2f", totalCommissions)));
            
            if (includeProjections) {
                BigDecimal projectedAmount = totalCommissions.multiply(new BigDecimal("1.15"));
                document.add(new Paragraph(String.format("Projected Next Month: %.2f", projectedAmount)));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate commission PDF report", e);
        }
    }

    /**
     * Generate Audit Report PDF
     */
    public byte[] generateAuditReport(LocalDate startDate, LocalDate endDate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            Paragraph title = new Paragraph("Audit Report - Returns and Corrections")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Date range
            Paragraph dateRange = new Paragraph(String.format("Report Period: %s to %s", 
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(dateRange);
            document.add(new Paragraph("\n"));

            // Get audit data (cancelled and rejected guarantees)
            List<GuaranteeContract> auditItems = guaranteeRepository.findAll().stream()
                    .filter(g -> g.getStatus().name().equals("CANCELLED") || g.getStatus().name().equals("REJECTED"))
                    .collect(Collectors.toList());

            // Create table
            Table table = new Table(UnitValue.createPercentArray(new float[]{20, 25, 15, 15, 25}))
                    .useAllAvailableWidth();

            // Headers
            table.addHeaderCell(new Cell().add(new Paragraph("Reference").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Applicant").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Last Modified").setBold()));

            // Data rows
            for (GuaranteeContract item : auditItems) {
                table.addCell(item.getReference());
                table.addCell(item.getApplicantId() != null ? "Client-" + item.getApplicantId() : "N/A");
                table.addCell(item.getStatus().toString());
                table.addCell(String.format("%.2f %s", item.getAmount(), item.getCurrency()));
                table.addCell(item.getLastModifiedDate() != null ? 
                    item.getLastModifiedDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A");
            }

            document.add(table);

            // Summary
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Summary:")
                    .setFontSize(14)
                    .setBold());
            
            long cancelled = auditItems.stream().filter(g -> g.getStatus().name().equals("CANCELLED")).count();
            long rejected = auditItems.stream().filter(g -> g.getStatus().name().equals("REJECTED")).count();
            
            document.add(new Paragraph(String.format("Total Cancelled: %d", cancelled)));
            document.add(new Paragraph(String.format("Total Rejected: %d", rejected)));
            document.add(new Paragraph(String.format("Total Audit Items: %d", auditItems.size())));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate audit PDF report", e);
        }
    }

    /**
     * Generate Expiry Alert Report PDF
     */
    public byte[] generateExpiryAlertReport(int daysAhead) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            LocalDate cutoffDate = LocalDate.now().plusDays(daysAhead);

            // Title
            Paragraph title = new Paragraph("Expiry Alert Report")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Date range
            Paragraph dateRange = new Paragraph(String.format("Guarantees expiring within %d days (by %s)", 
                    daysAhead, cutoffDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(dateRange);
            document.add(new Paragraph(String.format("Generated on: %s", 
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Get expiring guarantees
            List<GuaranteeContract> expiringGuarantees = guaranteeRepository.findAll().stream()
                    .filter(g -> g.getExpiryDate().isBefore(cutoffDate) || g.getExpiryDate().isEqual(cutoffDate))
                    .filter(g -> g.getStatus().name().equals("APPROVED") || g.getStatus().name().equals("SUBMITTED"))
                    .sorted((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()))
                    .collect(Collectors.toList());

            // Create table
            Table table = new Table(UnitValue.createPercentArray(new float[]{20, 20, 15, 15, 15, 15}))
                    .useAllAvailableWidth();

            // Headers
            table.addHeaderCell(new Cell().add(new Paragraph("Reference").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Applicant").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Currency").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Expiry Date").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Days Left").setBold()));

            // Data rows
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (GuaranteeContract guarantee : expiringGuarantees) {
                long daysLeft = LocalDate.now().until(guarantee.getExpiryDate()).getDays();
                
                table.addCell(guarantee.getReference());
                table.addCell(guarantee.getApplicantId() != null ? "Client-" + guarantee.getApplicantId() : "N/A");
                table.addCell(String.format("%.2f", guarantee.getAmount()));
                table.addCell(guarantee.getCurrency());
                table.addCell(guarantee.getExpiryDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                
                // Color code days left
                Cell daysLeftCell = new Cell().add(new Paragraph(String.valueOf(daysLeft)));
                if (daysLeft <= 7) {
                    daysLeftCell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
                }
                table.addCell(daysLeftCell);
                
                totalAmount = totalAmount.add(guarantee.getAmount());
            }

            document.add(table);

            // Summary
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Summary:")
                    .setFontSize(14)
                    .setBold());
            
            long critical = expiringGuarantees.stream()
                    .filter(g -> LocalDate.now().until(g.getExpiryDate()).getDays() <= 7)
                    .count();
            long warning = expiringGuarantees.stream()
                    .filter(g -> {
                        long days = LocalDate.now().until(g.getExpiryDate()).getDays();
                        return days > 7 && days <= 30;
                    })
                    .count();
            
            document.add(new Paragraph(String.format("Total Expiring Guarantees: %d", expiringGuarantees.size())));
            document.add(new Paragraph(String.format("Critical (≤ 7 days): %d", critical)));
            document.add(new Paragraph(String.format("Warning (8-30 days): %d", warning)));
            document.add(new Paragraph(String.format("Total Amount at Risk: %.2f", totalAmount)));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate expiry alert PDF report", e);
        }
    }
}
