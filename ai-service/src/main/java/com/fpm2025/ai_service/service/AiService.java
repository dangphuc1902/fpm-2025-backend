package com.fpm2025.ai_service.service;

import com.fpm2025.ai_service.dto.AiRequest;
import com.fpm2025.ai_service.dto.AiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class AiService {

    public AiResponse processNlp(AiRequest request) {
        log.info("Processing NLP for request: {}", request.getText());
        
        // This is a stub implementation. In a real application, you would call
        // OpenAI GPT, Google Gemini, or a local NLP model here.
        
        String text = request.getText().toLowerCase();
        BigDecimal amount = extractAmount(text);
        String category = extractCategory(text);
        
        return AiResponse.builder()
                .success(true)
                .intent("ADD_TRANSACTION")
                .amount(amount)
                .category(category)
                .transactionDate(LocalDateTime.now())
                .rawText(request.getText())
                .isAnomaly(false)
                .build();
    }

    public AiResponse detectAnomaly(AiRequest request) {
        log.info("Detecting anomalies for transaction descriptions: {}", request.getText());
        
        // Stub implementation: Detect large amounts or suspicious keywords
        boolean isAnomaly = request.getText().toLowerCase().contains("casino") || 
                            request.getText().toLowerCase().contains("urgent transfer");
                            
        return AiResponse.builder()
                .success(true)
                .isAnomaly(isAnomaly)
                .anomalyReason(isAnomaly ? "Suspicious keywords found in text" : null)
                .build();
    }

    private BigDecimal extractAmount(String text) {
        String numbersOnly = text.replaceAll("[^0-9]", "");
        if (numbersOnly.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(numbersOnly);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    private String extractCategory(String text) {
        if (text.contains("cà phê") || text.contains("coffee")) return "FOOD_AND_DINING";
        if (text.contains("taxi") || text.contains("grab")) return "TRANSPORTATION";
        if (text.contains("xem phim") || text.contains("movie")) return "ENTERTAINMENT";
        return "OTHER";
    }
}
