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
    
    private Long id;
    private Long userId;
    private ExportFormat format;
    private String period;
    private String status; // PENDING, PROCESSING, DONE, FAILED
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}