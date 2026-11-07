package com.fpm2025.ai_service.service;

import com.fpm2025.ai_service.dto.AiRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Khong co Gemini API key: AiService dung bo phan tich so tien theo quy tac. */
class AiServiceFallbackTest {

    private final AiService service = new AiService();

    @ParameterizedTest
    @CsvSource({
        "Toi uong cafe het 50k, 50000",
        "2 ly cafe 50k, 50000",
        "do xang 1.5tr, 1500000",
        "tien nha 3 trieu, 3000000",
        "mua sach 120.000 dong, 120000",
        "an trua 35000, 35000",
        "khong co so tien, 0"
    })
    void fallback_parsesVietnameseAmounts(String text, String expected) {
        AiRequest request = new AiRequest();
        request.setText(text);

        assertEquals(0, new BigDecimal(expected).compareTo(service.processNlp(request).getAmount()));
    }
}
