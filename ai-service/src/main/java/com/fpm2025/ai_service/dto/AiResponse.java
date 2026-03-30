package com.fpm2025.ai_service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AiResponse {
    private boolean success;
    private String intent;
    private BigDecimal amount;
    private String category;
    private LocalDateTime transactionDate;
    private String rawText;
    private String errorMessage;
    private boolean isAnomaly;
    private String anomalyReason;
}
