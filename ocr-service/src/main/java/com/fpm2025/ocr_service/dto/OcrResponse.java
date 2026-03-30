package com.fpm2025.ocr_service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OcrResponse {
    private String merchantName;
    private BigDecimal totalAmount;
    private LocalDateTime transactionDate;
    private String rawText;
    private boolean success;
    private String errorMessage;
}
