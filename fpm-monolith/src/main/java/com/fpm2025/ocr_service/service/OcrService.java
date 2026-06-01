package com.fpm2025.ocr_service.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fpm2025.ocr_service.dto.OcrResponse;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
@Slf4j
public class OcrService {

    @Value("${tesseract.datapath:/usr/share/tesseract-ocr/4.00/tessdata}")
    private String datapath;

    
    @Value("${tesseract.language:vie+eng}")
    private String language;

    public OcrResponse processReceipt(MultipartFile file) {
        log.info("Processing OCR via Native Tesseract for file: {}", file.getOriginalFilename());
        
        File tempFile = null;
        try {
            tempFile = convertMultiPartToFile(file);
            
            Tesseract tesseract = new Tesseract();
            tesseract.setLanguage(language);
            // Set the datapath directly to where apt-get installs it in the image
            tesseract.setDatapath(datapath);
            
            log.debug("Using Tesseract datapath: {}", datapath);
            String result = tesseract.doOCR(tempFile);
            
            return parseOcrResult(result);
            
        } catch (TesseractException | IOException | Error e) {
            log.error("OCR Production Error: {}", e.getMessage());
            return OcrResponse.builder()
                .success(false)
                .errorMessage("OCR Library Error: " + e.getMessage())
                .build();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("ocr_", "_" + System.currentTimeMillis());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    private OcrResponse parseOcrResult(String rawText) {
        BigDecimal totalAmount = extractAmount(rawText);
        String merchant = extractMerchant(rawText);
        
        return OcrResponse.builder()
            .rawText(rawText)
            .success(true)
            .totalAmount(totalAmount)
            .merchantName(merchant)
            .transactionDate(LocalDateTime.now()) 
            .build();
    }
    
    private BigDecimal extractAmount(String text) {
        // Regex optimized for common Vietnamese receipt formats
        // Matches "TONG CONG", "THANH TOAN", "VALUE", etc. followed by numbers
        Pattern pattern = Pattern.compile("(TỔNG CỘNG|TOTAL|THANH TOÁN|VALUE|CỘNG)\\s*[:\\-]?\\s*([\\d,\\.\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        BigDecimal maxAmount = BigDecimal.ZERO;
        while (matcher.find()) {
            String amountStr = matcher.group(2).replaceAll("[^0-9]", "");
            if (!amountStr.isEmpty()) {
                try {
                    BigDecimal val = new BigDecimal(amountStr);
                    if (val.compareTo(maxAmount) > 0) maxAmount = val;
                } catch (Exception ignored) {}
            }
        }
        return maxAmount;
    }
    
    private String extractMerchant(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().length() > 3) return line.trim(); // Assume first non-empty line is merchant
        }
        return "Unknown Merchant";
    }
}
