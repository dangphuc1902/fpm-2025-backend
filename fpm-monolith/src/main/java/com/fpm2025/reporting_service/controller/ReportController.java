package com.fpm2025.reporting_service.controller;

import com.fpm2025.reporting_service.dto.response.BaseResponse;
import com.fpm2025.reporting_service.domain.valueobject.ExportFormat;
import com.fpm2025.reporting_service.dto.request.ReportRequest;
import com.fpm2025.reporting_service.dto.response.ReportResponse;
import com.fpm2025.reporting_service.service.ReportingService;

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
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate month) {
        
        log.info("Getting monthly report for user: {}, month: {}", userId, month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(userId)
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
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Exporting PDF report for user: {}, month: {}", userId, month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(userId)
                .startDate(month.withDayOfMonth(1))
                .endDate(month.withDayOfMonth(month.lengthOfMonth()))
                .format(ExportFormat.PDF)
                .build();
        
        ReportResponse res = reportingService.generateMonthlyReport(request);
        byte[] pdfData = reportingService.downloadReport(res.getFileUrl());
        
        ByteArrayResource resource = new ByteArrayResource(pdfData);
        
        String filename = String.format("report_%s_%s.pdf", 
                userId, 
                month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> exportExcel(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        log.info("Exporting Excel report for user: {}, month: {}", userId, month);
        
        ReportRequest request = ReportRequest.builder()
                .userId(userId)
                .startDate(month.withDayOfMonth(1))
                .endDate(month.withDayOfMonth(month.lengthOfMonth()))
                .format(ExportFormat.EXCEL)
                .build();
        
        ReportResponse res = reportingService.generateMonthlyReport(request);
        byte[] excelData = reportingService.downloadReport(res.getFileUrl());
        
        ByteArrayResource resource = new ByteArrayResource(excelData);
        
        String filename = String.format("report_%s_%s.xlsx", 
                userId, 
                month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    // =========================================================================
    // GET /api/v1/reports/spending-by-category
    // Biểu đồ tròn (Pie Chart): chi tiêu theo danh mục trong tháng
    // =========================================================================
    @GetMapping("/spending-by-category")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse<?> getSpendingByCategory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false, defaultValue = "EXPENSE") String type) {

        String month = (yearMonth != null && !yearMonth.isBlank())
                ? yearMonth
                : java.time.YearMonth.now().toString();

        log.info("spending-by-category: userId={}, month={}, type={}", userId, month, type);

        var chartData = reportingService.getSpendingByCategory(userId, month, type);
        return BaseResponse.success(chartData);
    }

    // =========================================================================
    // GET /api/v1/reports/trends
    // Biểu đồ đường: So sánh income vs expense theo N tháng gần nhất (default 6)
    // =========================================================================
    @GetMapping("/trends")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse<?> getTrends(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false, defaultValue = "6") int months) {

        log.info("trends: userId={}, months={}", userId, months);

        var trendData = reportingService.getMonthlyTrends(userId, months);
        return BaseResponse.success(trendData);
    }

    // =========================================================================
    // GET /api/v1/reports/budget-comparison
    // So sánh ngân sách đặt ra vs thực tế chi tiêu theo danh mục
    // =========================================================================
    @GetMapping("/budget-comparison")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse<?> getBudgetComparison(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String yearMonth) {

        String month = (yearMonth != null && !yearMonth.isBlank())
                ? yearMonth
                : java.time.YearMonth.now().toString();

        log.info("budget-comparison: userId={}, month={}", userId, month);
 
         var comparisonData = reportingService.getBudgetComparison(userId, month);
         return BaseResponse.success(comparisonData);
     }
 
     @PostMapping("/export")
     @PreAuthorize("isAuthenticated()")
     public BaseResponse<Long> exportAsync(
             @AuthenticationPrincipal Long userId,
             @RequestBody ReportRequest request) {
         
         log.info("Requesting async export for user: {}, format: {}", userId, request.getFormat());
         request.setUserId(userId);
         
         Long jobId = reportingService.submitExportJob(userId, request);
         return BaseResponse.success(jobId, "Export job submitted successfully. Use its ID to check status.");
     }
 
     @GetMapping("/export/{jobId}")
     @PreAuthorize("isAuthenticated()")
     public BaseResponse<com.fpm2025.reporting_service.domain.model.ExportJob> getExportStatus(
             @AuthenticationPrincipal Long userId,
             @PathVariable Long jobId) {
         
         return BaseResponse.success(reportingService.getExportJobStatus(jobId, userId));
     }
 
     @GetMapping("/export/{jobId}/download")
     @PreAuthorize("isAuthenticated()")
     public ResponseEntity<Resource> downloadExportResult(
             @AuthenticationPrincipal Long userId,
             @PathVariable Long jobId) {
         
         com.fpm2025.reporting_service.domain.model.ExportJob job = reportingService.getExportJobStatus(jobId, userId);
         
         if (!"DONE".equals(job.getStatus().name())) {
             return ResponseEntity.badRequest().build();
         }
         
         byte[] data = reportingService.downloadReport(job.getFileUrl());
         ByteArrayResource resource = new ByteArrayResource(data);
         
         String contentType = job.getFormat() == ExportFormat.PDF ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
         
         return ResponseEntity.ok()
                 .contentType(MediaType.parseMediaType(contentType))
                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getFileName() + "\"")
                 .body(resource);
     }

    @GetMapping("/insights")
    @PreAuthorize("isAuthenticated()")
    public BaseResponse<String> getInsights(@AuthenticationPrincipal Long userId) {
        return BaseResponse.success("AI Insights upcoming!");
    }
}
