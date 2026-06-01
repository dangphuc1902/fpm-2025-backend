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

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String apiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    public AiResponse processNlp(AiRequest request) {
        log.info("Processing NLP via Gemini for: {}", request.getText());
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key is missing, falling back to rule-based.");
            return fallbackNlp(request);
        }

        try {
            String prompt = "Analyze this financial record in Vietnamese: \"" + request.getText() + "\". " +
                    "Return a JSON object with these fields: " +
                    "\"amount\" (number, use 0 if not found), " +
                    "\"category\" (string, select from: FOOD_AND_DINING, TRANSPORTATION, ENTERTAINMENT, SHOPPING, UTILITIES, HEALTH, OTHER), " +
                    "\"intent\" (string, e.g., ADD_TRANSACTION), " +
                    "\"transactionDate\" (string, ISO 8601). " +
                    "Return ONLY the JSON.";

            String aiResponse = callGemini(prompt);
            return parseGeminiResponse(aiResponse, request.getText());
        } catch (Exception e) {
            log.error("Gemini API error: {}", e.getMessage());
            return fallbackNlp(request);
        }
    }

    public AiResponse detectAnomaly(AiRequest request) {
        log.info("Detecting anomalies via Gemini: {}", request.getText());
        
        if (apiKey == null || apiKey.isEmpty()) {
            return AiResponse.builder().success(true).isAnomaly(false).build();
        }

        try {
            String prompt = "Evaluate if this financial description is an anomaly or suspicious: \"" + request.getText() + "\". " +
                    "Return JSON: {\"isAnomaly\": boolean, \"reason\": \"string\"}.";
            
            String responseStr = callGemini(prompt);
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseStr);
            String content = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            // Clean markdown blocks if present
            content = content.replaceAll("```json|```", "").trim();
            com.fasterxml.jackson.databind.JsonNode result = new com.fasterxml.jackson.databind.ObjectMapper().readTree(content);

            return AiResponse.builder()
                    .success(true)
                    .isAnomaly(result.path("isAnomaly").asBoolean())
                    .anomalyReason(result.path("reason").asText())
                    .build();
        } catch (Exception e) {
            return AiResponse.builder().success(false).errorMessage(e.getMessage()).build();
        }
    }

    public AiResponse chat(AiRequest request) {
        log.info("Chatting via Gemini: {}", request.getText());
        
        if (apiKey == null || apiKey.isEmpty()) {
            return AiResponse.builder().success(false).errorMessage("Gemini API key not configured").build();
        }

        try {
            String prompt = "You are a friendly personal finance assistant in FPM App. Answer this question in Vietnamese: \"" + request.getText() + "\". Keep it concise and helpful.";
            
            String responseStr = callGemini(prompt);
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseStr);
            String aiAnswer = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            return AiResponse.builder()
                    .success(true)
                    .rawText(aiAnswer)
                    .build();
        } catch (Exception e) {
            return AiResponse.builder().success(false).errorMessage(e.getMessage()).build();
        }
    }

    private String callGemini(String prompt) {
        java.util.Map<String, Object> requestBody = java.util.Map.of(
            "contents", java.util.List.of(
                java.util.Map.of("parts", java.util.List.of(
                    java.util.Map.of("text", prompt)
                ))
            )
        );

        return restTemplate.postForObject(GEMINI_URL + apiKey, requestBody, String.class);
    }

    private AiResponse parseGeminiResponse(String responseJson, String rawText) throws Exception {
        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseJson);
        String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        
        // Remove JSON markdown wrappers if AI provides them
        text = text.replaceAll("```json|```", "").trim();
        
        com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(text);
        
        return AiResponse.builder()
                .success(true)
                .intent(node.path("intent").asText("ADD_TRANSACTION"))
                .amount(new BigDecimal(node.path("amount").asText("0")))
                .category(node.path("category").asText("OTHER"))
                .transactionDate(LocalDateTime.now()) // AI might provide one, but we use now as safety
                .rawText(rawText)
                .build();
    }

    private AiResponse fallbackNlp(AiRequest request) {
        // ... (Keep simpler regex logic here as fallback)
        String text = request.getText().toLowerCase();
        return AiResponse.builder()
                .success(true)
                .intent("ADD_TRANSACTION")
                .amount(new BigDecimal(text.replaceAll("[^0-9]", "").isEmpty() ? "0" : text.replaceAll("[^0-9]", "")))
                .category("OTHER")
                .rawText(request.getText())
                .build();
    }
}
