package com.fpm2025.ocr_service.controller;

import com.fpm2025.ocr_service.dto.OcrResponse;
import com.fpm2025.ocr_service.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR API", description = "Các API phục vụ trích xuất dữ liệu hoá đơn.")
public class OcrController {

    private final OcrService ocrService;

    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    @Operation(summary = "Trích xuất thông tin hoá đơn", description = "Dùng Tesseract hoặc Google Vision để đọc thông tin hoá đơn")
    public ResponseEntity<OcrResponse> extractReceipt(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(OcrResponse.builder()
                .success(false)
                .errorMessage("No file provided.")
                .build());
        }

        OcrResponse response = ocrService.processReceipt(file);
        return ResponseEntity.ok(response);
    }
}
