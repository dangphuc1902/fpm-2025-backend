package com.fpm_2025.reportingservice.controller;

import com.fpm_2025.core.annotation.CurrentUser;
import com.fpm_2025.core.dto.BaseResponse;
import com.fpm_2025.reporting.service.dto.request.ReportRequest;
import com.fpm_2025.reporting.service.dto.response.ReportResponse;
import com.fpm_2025.reporting.service.service.PdfExportService;
import com.fpm_2025.reporting.service.service.ExcelExportService;
import com.fpm_2025.reporting.service.service.ReportGenerationService;
import com.fpm_2025.security.user.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportGenerationService reportGenerationService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    @GetMapping("/monthly")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse<ReportResponse> getMonthlyReport(
            @CurrentUser UserPrincipal user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Getting monthly report for user: {}, month: {}", user.getId(), month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(user.getId())
                .startDate(month.withDayOfMonth(1))
                .endDate(month.withDayOfMonth(month.lengthOfMonth()))
                .build();
        
        ReportResponse response = reportGenerationService.generateMonthlyReport(request);
        
        return BaseResponse.success("Monthly report generated successfully", response);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> exportPdf(
            @CurrentUser UserPrincipal user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Exporting PDF report for user: {}, month: {}", user.getId(), month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(user.getId())
                .startDate(month.withDayOfMonth(1))
                .endDate(month.withDayOfMonth(month.lengthOfMonth()))
                .build();
        
        byte[] pdfData = pdfExportService.generatePdfReport(request);
        
        ByteArrayResource resource = new ByteArrayResource(pdfData);
        
        String filename = String.format("report_%s_%s.pdf", 
                user.getId(), 
                month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> exportExcel(
            @CurrentUser UserPrincipal user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Exporting Excel report for user: {}, month: {}", user.getId(), month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(user.getId())
                .startDate(month.withDayOfMonth(1))
                .endDate(month.withDayOfMonth(month.lengthOfMonth()))
                .build();
        
        byte[] excelData = excelExportService.generateExcelReport(request);
        
        ByteArrayResource resource = new ByteArrayResource(excelData);
        
        String filename = String.format("report_%s_%s.xlsx", 
                user.getId(), 
                month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}