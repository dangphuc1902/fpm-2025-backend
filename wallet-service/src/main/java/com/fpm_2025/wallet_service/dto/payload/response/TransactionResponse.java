package com.fpm_2025.wallet_service.dto.payload.response;

import com.fpm_2025.wallet_service.entity.enums.CategoryType;
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
public class TransactionResponse {
    private Long id;
    private Long userId;
    private Long walletId;
    private String walletName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private CategoryType type;
    private String note;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}