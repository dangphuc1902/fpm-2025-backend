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
class BalanceUpdateEvent implements Serializable {
    private Long walletId;
    private Long userId;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private BigDecimal changeAmount;
    private String reason;
    private Instant timestamp;
}

