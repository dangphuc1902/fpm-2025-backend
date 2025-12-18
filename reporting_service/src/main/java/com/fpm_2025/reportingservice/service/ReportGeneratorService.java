package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Report Generator Service
 * 
 * Generates financial reports in various formats:
 * - PDF: Professional layout with charts
 * - Excel: Detailed data with formulas
 * - CSV: Raw data for external processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGeneratorService {

    @Value("${report.storage.path:/tmp/reports}")
    private String storagePath;

    private static final NumberFormat CURRENCY_FORMAT = 
        NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Generate report based on format
     */
    public byte[] generate(
            List<TransactionData> transactions,
            MonthlyStatistics stats,
            List<WalletData> wallets,
            ReportFormat format) {
        
        log.info("Generating {} report with {} transactions", 
            format, transactions.size());

        return switch (format) {
            case PDF -> generatePDF(transactions, stats, wallets);
            case EXCEL -> generateExcel(transactions, stats, wallets);
            case CSV -> generateCSV(transactions);
        };
    }

    // ==================== PDF Generation ====================

    private byte[] generatePDF(
            List<TransactionData> transactions,
            MonthlyStatistics stats,
            List<WalletData> wallets) {
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph(
                "BÁO CÁO TÀI CHÍNH THÁNG " + stats.getMonth(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Summary Section
            addSummarySection(document, stats);
            document.add(new Paragraph("\n"));

            // Wallets Section
            addWalletsSection(document, wallets);
            document.add(new Paragraph("\n"));

            // Transactions Table
            addTransactionsTable(document, transactions);
            document.add(new Paragraph("\n"));

            // Footer
            Font footerFont = FontFactory.getFont(
                FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph footer = new Paragraph(
                "Tạo bởi FPM System - " + 
                java.time.LocalDateTime.now().format(DATE_FORMAT), 
                footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addSummarySection(Document document, MonthlyStatistics stats) 
            throws DocumentException {
        
        Font sectionFont = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Paragraph section = new Paragraph("TỔNG QUAN", sectionFont);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addSummaryRow(table, "Tổng thu nhập:", 
            formatCurrency(stats.getTotalIncome()));
        addSummaryRow(table, "Tổng chi tiêu:", 
            formatCurrency(stats.getTotalExpense()));
        addSummaryRow(table, "Thu nhập ròng:", 
            formatCurrency(stats.getNetIncome()));
        addSummaryRow(table, "Số giao dịch:", 
            String.valueOf(stats.getTransactionCount()));
        addSummaryRow(table, "Chi tiêu TB/ngày:", 
            formatCurrency(stats.getAvgDailyExpense()));
        addSummaryRow(table, "Danh mục chi nhiều nhất:", 
            stats.getTopExpenseCategory());

        document.add(table);
    }

    private void addWalletsSection(Document document, List<WalletData> wallets) 
            throws DocumentException {
        
        Font sectionFont = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Paragraph section = new Paragraph("VÍ TIỀN", sectionFont);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);

        // Header
        addTableHeader(table, "Tên ví", "Loại", "Số dư");

        // Data
        for (WalletData wallet : wallets) {
            addTableCell(table, wallet.getName());
            addTableCell(table, wallet.getType());
            addTableCell(table, formatCurrency(wallet.getBalance()));
        }

        document.add(table);
    }

    private void addTransactionsTable(
            Document document, 
            List<TransactionData> transactions) 
            throws DocumentException {
        
        Font sectionFont = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Paragraph section = new Paragraph("GIAO DỊCH", sectionFont);
        section.setSpacingAfter(10);
        document.add(section);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 2, 1, 1, 2});

        // Header
        addTableHeader(table, "Ngày", "Danh mục", "Loại", "Số tiền", "Ghi chú");

        // Data
        for (TransactionData tx : transactions) {
            addTableCell(table, tx.getTransactionDate().format(DATE_FORMAT));
            addTableCell(table, tx.getCategoryName());
            addTableCell(table, tx.getType());
            
            PdfPCell amountCell = new PdfPCell(new Phrase(
                formatCurrency(tx.getAmount())));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if ("EXPENSE".equals(tx.getType())) {
                amountCell.setBackgroundColor(new BaseColor(255, 230, 230));
            } else {
                amountCell.setBackgroundColor(new BaseColor(230, 255, 230));
            }
            table.addCell(amountCell);
            
            addTableCell(table, tx.getNote() != null ? tx.getNote() : "");
        }

        document.add(table);
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPaddingRight(10);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font headerFont = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.DARK_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setPadding(5);
        table.addCell(cell);
    }

    // ==================== Excel Generation ====================

    private byte[] generateExcel(
            List<TransactionData> transactions,
            MonthlyStatistics stats,
            List<WalletData> wallets) {
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Summary Sheet
            Sheet summarySheet = workbook.createSheet("Tổng quan");
            createSummarySheet(summarySheet, stats, workbook);

            // Wallets Sheet
            Sheet walletsSheet = workbook.createSheet("Ví tiền");
            createWalletsSheet(walletsSheet, wallets, workbook);

            // Transactions Sheet
            Sheet txSheet = workbook.createSheet("Giao dịch");
            createTransactionsSheet(txSheet, transactions, workbook);

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate Excel report", e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    private void createSummarySheet(
            Sheet sheet, 
            MonthlyStatistics stats, 
            Workbook workbook) {
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO TÀI CHÍNH THÁNG " + stats.getMonth());
        
        rowNum++; // Empty row

        // Data
        addExcelRow(sheet, rowNum++, headerStyle, dataStyle, 
            "Tổng thu nhập", stats.getTotalIncome());
        addExcelRow(sheet, rowNum++, headerStyle, dataStyle, 
            "Tổng chi tiêu", stats.getTotalExpense());
        addExcelRow(sheet, rowNum++, headerStyle, dataStyle, 
            "Thu nhập ròng", stats.getNetIncome());
        addExcelRow(sheet, rowNum++, headerStyle, dataStyle, 
            "Số giao dịch", stats.getTransactionCount());

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createWalletsSheet(
            Sheet sheet, 
            List<WalletData> wallets, 
            Workbook workbook) {
        
        CellStyle headerStyle = createHeaderStyle(workbook);

        // Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Tên ví", "Loại", "Số dư"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        int rowNum = 1;
        for (WalletData wallet : wallets) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(wallet.getName());
            row.createCell(1).setCellValue(wallet.getType());
            row.createCell(2).setCellValue(wallet.getBalance().doubleValue());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createTransactionsSheet(
            Sheet sheet, 
            List<TransactionData> transactions, 
            Workbook workbook) {
        
        CellStyle headerStyle = createHeaderStyle(workbook);

        // Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Ngày", "Ví", "Danh mục", "Loại", "Số tiền", "Ghi chú"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        int rowNum = 1;
        for (TransactionData tx : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(
                tx.getTransactionDate().format(DATE_FORMAT));
            row.createCell(1).setCellValue(tx.getWalletName());
            row.createCell(2).setCellValue(tx.getCategoryName());
            row.createCell(3).setCellValue(tx.getType());
            row.createCell(4).setCellValue(tx.getAmount().doubleValue());
            row.createCell(5).setCellValue(
                tx.getNote() != null ? tx.getNote() : "");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        return workbook.createCellStyle();
    }

    private void addExcelRow(
            Sheet sheet, int rowNum, 
            CellStyle headerStyle, CellStyle dataStyle,
            String label, Object value) {
        
        Row row = sheet.createRow(rowNum);
        
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);

        Cell valueCell = row.createCell(1);
        if (value instanceof BigDecimal) {
            valueCell.setCellValue(((BigDecimal) value).doubleValue());
        } else {
            valueCell.setCellValue(String.valueOf(value));
        }
        valueCell.setCellStyle(dataStyle);
    }

    // ==================== CSV Generation ====================

    private byte[] generateCSV(List<TransactionData> transactions) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(baos);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("Ngày", "Ví", "Danh mục", "Loại", "Số tiền", "Ghi chú"))) {

            for (TransactionData tx : transactions) {
                csvPrinter.printRecord(
                    tx.getTransactionDate().format(DATE_FORMAT),
                    tx.getWalletName(),
                    tx.getCategoryName(),
                    tx.getType(),
                    tx.getAmount().toString(),
                    tx.getNote() != null ? tx.getNote() : ""
                );
            }

            csvPrinter.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate CSV report", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    // ==================== Storage ====================

    public String uploadToStorage(byte[] data, String fileName) {
        try {
            Files.createDirectories(Paths.get(storagePath));
            String filePath = storagePath + "/" + fileName;
            Files.write(Paths.get(filePath), data);
            return "/reports/" + fileName; // URL for download
        } catch (IOException e) {
            log.error("Failed to upload report to storage", e);
            throw new RuntimeException("Storage upload failed", e);
        }
    }

    public byte[] downloadFromStorage(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String filePath = storagePath + "/" + fileName;
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Failed to download report from storage", e);
            throw new RuntimeException("Storage download failed", e);
        }
    }

    // ==================== Utilities ====================

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}