package com.fpm2025.transaction_service.dto;

import com.fpm2025.transaction_service.entity.enums.TransactionStatus;
import com.fpm2025.transaction_service.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private Long userId;
    private Long walletId;
    private Long categoryId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private LocalDateTime transactionDate;
    private String description;
    private String note;
    private String location;
    private Boolean isRecurring;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
