package com.fpm2025.ocr_service.service;

import com.fpm2025.ocr_service.dto.OcrResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcrServiceTest {

    private final OcrService service = new OcrService();

    private BigDecimal totalOf(String receiptText) {
        // Tep .txt di thang vao buoc phan tich, khong can Tesseract
        MockMultipartFile file = new MockMultipartFile("file", "bill.txt", "text/plain", receiptText.getBytes());
        OcrResponse response = service.processReceipt(file);
        return response.getTotalAmount();
    }

    @Test
    void imageSuffix_detectsJpegFromContent() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

        assertEquals(".jpg", OcrService.imageSuffix(jpeg));
    }

    @Test
    void imageSuffix_detectsPngFromContent() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47};

        assertEquals(".png", OcrService.imageSuffix(png));
    }

    @Test
    void total_readsKeywordWithoutDiacritics() {
        assertEquals(new BigDecimal("50000"), totalOf("Ca phe sua 50,000\nTONG CONG: 50000 VND"));
    }

    @Test
    void total_readsKeywordWithDiacritics() {
        assertEquals(new BigDecimal("250000"), totalOf("TỔNG TIỀN: 250.000 đ"));
    }

    @Test
    void total_prefersAmountToPayOverSubtotalBeforeDiscount() {
        assertEquals(new BigDecimal("130000"),
                totalOf("Tổng cộng: 136,500\nGiảm giá: 6,500\nTHANH TOÁN: 130,000\nTiền khách đưa: 200,000"));
    }

    @Test
    void total_doesNotMergeNumbersFromNextLine() {
        assertEquals(new BigDecimal("120000"), totalOf("Tổng cộng: 120,000\n24/10/2026"));
    }
}
