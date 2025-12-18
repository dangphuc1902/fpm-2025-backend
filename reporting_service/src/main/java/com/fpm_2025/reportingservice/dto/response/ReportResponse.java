package com.fpm_2025.reportingservice.dto.response;

import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    
    private String reportId;
    private String reportName;
    private ExportFormat format;
    private String status; // GENERATING, COMPLETED, FAILED
    private String downloadUrl;
    private Long fileSizeBytes;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private Integer progressPercentage;
    private String errorMessage;
}