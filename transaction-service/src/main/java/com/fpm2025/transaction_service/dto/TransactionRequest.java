package com.fpm2025.transaction_service.dto;

import com.fpm2025.transaction_service.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionRequest {
    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    private Long categoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String currency = "VND";

    @NotNull(message = "Type is required")
    private TransactionType type;

    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    private String description;
    private String note;
    private String location;
    private Boolean isRecurring = false;
}
