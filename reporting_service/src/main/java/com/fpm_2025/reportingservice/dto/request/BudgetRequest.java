package com.fpm_2025.reportingservice.dto.request;

import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRequest {
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String categoryName;
    
    @NotNull(message = "Amount limit is required")
    @Positive(message = "Amount limit must be positive")
    private BigDecimal amountLimit;
    
    @NotNull(message = "Budget period is required")
    private BudgetPeriod period;
    
    @NotBlank(message = "Year month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Year month must be in format yyyy-MM")
    private String yearMonth;
}