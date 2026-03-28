package com.fpm2025.transaction_service.dto;

import com.fpm2025.transaction_service.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO cho việc cập nhật giao dịch (PUT /api/v1/transactions/{id}).
 * Tất cả các trường đều optional — chỉ field nào không null mới được update.
 */
@Data
public class UpdateTransactionRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String currency;

    private TransactionType type;

    private Long categoryId;

    private LocalDateTime transactionDate;

    private String description;

    private String note;

    private String location;

    private Boolean isRecurring;
}
