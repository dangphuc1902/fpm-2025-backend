package com.fpm_2025.reportingservice.service;

import com.fpm2025.reporting.service.dto.request.ReportRequest;
import com.fpm2025.reporting.service.dto.response.ReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ReportGenerationService reportGenerationService;

    public byte[] generateExcelReport(ReportRequest request) {
        log.info("Generating Excel report for user: {}", request.getUserId());

        ReportResponse reportData = reportGenerationService.generateMonthlyReport(request);

        try (Workbook workbook = new XSSFWorkbook()) {
            // Summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, reportData);

            // Transactions sheet
            Sheet transactionsSheet = workbook.createSheet("Transactions");
            createTransactionsSheet(transactionsSheet, reportData);

            // Category breakdown sheet
            Sheet categorySheet = workbook.createSheet("Categories");
            createCategorySheet(categorySheet, reportData);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            log.info("Excel report generated successfully");
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error generating Excel report", e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private void createSummarySheet(Sheet sheet, ReportResponse reportData) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Monthly Report - " + 
                reportData.getMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        titleCell.setCellStyle(headerStyle);

        rowNum++; // Empty row

        // Summary data
        createDataRow(sheet, rowNum++, "Total Income:", reportData.getTotalIncome().toString());
        createDataRow(sheet, rowNum++, "Total Expense:", reportData.getTotalExpense().toString());
        createDataRow(sheet, rowNum++, "Balance:", reportData.getBalance().toString());
        createDataRow(sheet, rowNum++, "Transaction Count:", String.valueOf(reportData.getTransactionCount()));

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createTransactionsSheet(Sheet sheet, ReportResponse reportData) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());

        int rowNum = 0;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Description", "Category", "Amount", "Type"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        if (reportData.getTransactions() != null) {
            for (var transaction : reportData.getTransactions()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(transaction.getTransactionDate().toString());
                row.createCell(1).setCellValue(transaction.getDescription());
                row.createCell(2).setCellValue(transaction.getCategoryName());
                row.createCell(3).setCellValue(transaction.getAmount().doubleValue());
                row.createCell(4).setCellValue(transaction.getType());
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCategorySheet(Sheet sheet, ReportResponse reportData) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());

        int rowNum = 0;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Category", "Amount", "Percentage"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        if (reportData.getCategoryBreakdown() != null) {
            for (var category : reportData.getCategoryBreakdown()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(category.getCategoryName());
                row.createCell(1).setCellValue(category.getAmount().doubleValue());
                row.createCell(2).setCellValue(category.getPercentage() + "%");
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createDataRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}