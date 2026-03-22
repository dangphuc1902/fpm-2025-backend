package com.fpm_2025.reportingservice.controller;

import com.fpm_2025.reportingservice.dto.response.BaseResponse;
import com.fpm_2025.reportingservice.security.UserPrincipal;
import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;
import com.fpm_2025.reportingservice.dto.request.ReportRequest;
import com.fpm_2025.reportingservice.dto.response.ReportResponse;
import com.fpm_2025.reportingservice.service.ReportingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportingService reportingService;

    @GetMapping("/monthly")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse<ReportResponse> getMonthlyReport(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Getting monthly report for user: {}, month: {}", user.getId(), month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(user.getId())
                .startDate(month.withDayOfMonth(1))
                .endDate(month.withDayOfMonth(month.lengthOfMonth()))
                .format(ExportFormat.PDF)
                .build();
        
        ReportResponse response = reportingService.generateMonthlyReport(request);
        
        return BaseResponse.success(response);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> exportPdf(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Exporting PDF report for user: {}, month: {}", user.getId(), month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(user.getId())
                .startDate(month.withDayOfMonth(1))
                .endDate(month.withDayOfMonth(month.lengthOfMonth()))
                .format(ExportFormat.PDF)
                .build();
        
        ReportResponse res = reportingService.generateMonthlyReport(request);
        byte[] pdfData = reportingService.downloadReport(res.getFileUrl());
        
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
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Exporting Excel report for user: {}, month: {}", user.getId(), month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(user.getId())
                .startDate(month.withDayOfMonth(1))
                .endDate(month.withDayOfMonth(month.lengthOfMonth()))
                .format(ExportFormat.EXCEL)
                .build();
        
        ReportResponse res = reportingService.generateMonthlyReport(request);
        byte[] excelData = reportingService.downloadReport(res.getFileUrl());
        
        ByteArrayResource resource = new ByteArrayResource(excelData);
        
        String filename = String.format("report_%s_%s.xlsx", 
                user.getId(), 
                month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/insights")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse<String> getInsights(@AuthenticationPrincipal UserPrincipal user) {
        return BaseResponse.success("AI Insights upcoming!");
    }
}