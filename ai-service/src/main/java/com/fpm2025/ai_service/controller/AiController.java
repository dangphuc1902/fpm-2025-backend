package com.fpm2025.ai_service.controller;

import com.fpm2025.ai_service.dto.AiRequest;
import com.fpm2025.ai_service.dto.AiResponse;
import com.fpm2025.ai_service.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Service API", description = "Các API phục vụ NLP, phân loại giao dịch bằng AI, tự động gợi ý")
public class AiController {

    private final AiService aiService;

    @PostMapping("/nlp")
    @Operation(summary = "Phân tích NLP để trích xuất giao dịch", description = "Đưa vào một câu văn mẫu (VD: 'Tôi uống cafe hết 50k') để tách xuất số tiền, danh mục.")
    public ResponseEntity<AiResponse> analyzeNlp(@RequestBody AiRequest request) {
        if (request.getText() == null || request.getText().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(aiService.processNlp(request));
    }

    @PostMapping("/anomaly")
    @Operation(summary = "Phát hiện nội dung bất thường", description = "Detect giao dịch đáng ngờ dựa trên mô hình học máy (Gemini AI)")
    public ResponseEntity<AiResponse> checkAnomaly(@RequestBody AiRequest request) {
        if (request.getText() == null || request.getText().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(aiService.detectAnomaly(request));
    }

    @PostMapping("/chat")
    @Operation(summary = "Trò chuyện với trợ lý Gemini", description = "Hỏi bất kỳ câu hỏi nào về tài chính hoặc sử dụng app, Gemini sẽ trả lời.")
    public ResponseEntity<AiResponse> chat(@RequestBody AiRequest request) {
        if (request.getText() == null || request.getText().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(aiService.chat(request));
    }
}
