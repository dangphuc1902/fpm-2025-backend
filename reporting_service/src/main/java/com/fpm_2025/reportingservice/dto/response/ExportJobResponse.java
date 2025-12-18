package com.fpm_2025.reportingservice.dto.response;

import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;
import com.fpm_2025.reportingservice.domain.valueobject.ExportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobResponse {
    
    private String jobId;
    private ExportFormat format;
    private ExportStatus status;
    private String downloadUrl;
    private Long fileSize;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Integer progressPercentage;
}