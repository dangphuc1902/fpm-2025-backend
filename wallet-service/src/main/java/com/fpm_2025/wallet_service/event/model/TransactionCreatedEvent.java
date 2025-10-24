package com.fpm_2025.wallet_service.event.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TransactionCreatedEvent implements Serializable {
    private Long transactionId;
    private Long walletId;
    private Long userId;
    private Long categoryId;
    private BigDecimal amount;
    private String type;
    private String note;
    private Instant timestamp;
}