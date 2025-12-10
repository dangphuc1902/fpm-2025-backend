package com.fpm_2025.reportingservice.service;

import com.fpm2025.reporting.service.dto.request.ReportRequest;
import com.fpm2025.reporting.service.dto.response.ReportResponse;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final ReportGenerationService reportGenerationService;
    private final TemplateEngine templateEngine;

    public byte[] generatePdfReport(ReportRequest request) {
        log.info("Generating PDF report for user: {}", request.getUserId());

        // Generate report data
        ReportResponse reportData = reportGenerationService.generateMonthlyReport(request);

        // Create Thymeleaf context
        Context context = new Context();
        context.setVariable("report", reportData);
        context.setVariable("month", request.getStartDate().format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        // Process template
        String html = templateEngine.process("report-template", context);

        // Convert to PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);

        log.info("PDF report generated successfully");
        return outputStream.toByteArray();
    }
}
