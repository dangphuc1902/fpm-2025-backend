package com.fpm_2025.wallet_service.dto.payload.request;

import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransactionRequest {

    private Long categoryId;

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private CategoryType type;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    private LocalDateTime transactionDate;
}