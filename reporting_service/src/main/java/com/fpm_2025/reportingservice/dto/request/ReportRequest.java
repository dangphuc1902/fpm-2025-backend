package com.fpm_2025.reportingservice.dto.request;

import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    
    private Long userId;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    private Long walletId;
    
    private List<Long> categoryIds;
    
    @NotNull(message = "Export format is required")
    private ExportFormat format;
    
    @Builder.Default
    private Boolean includeSummary = true;
    
    @Builder.Default
    private Boolean includeTransactions = true;
    
    @Builder.Default
    private Boolean includeCharts = false;
    
    @Builder.Default
    private Boolean includeBudgetComparison = false;
    
    @Builder.Default
    private Boolean includeTrends = false;
    
    private String reportName;
}