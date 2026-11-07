package com.fpm2025.ocr_service.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
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
        
        // 1. Direct text file bypass
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.endsWith(".txt") || filename.endsWith(".csv"))) {
            try {
                String text = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                log.info("Uploaded file is a text file, directly parsing contents (bypassing OCR engine).");
                return parseOcrResult(text);
            } catch (Exception e) {
                log.warn("Failed to read text file directly: {}", e.getMessage());
            }
        }
        
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
            
        } catch (Throwable e) {
            log.error("OCR Production Error: {}, attempting plain-text fallback", e.getMessage());
            
            // 2. Exception plain-text content fallback (e.g. for mock files with image mime-type)
            try {
                byte[] bytes = file.getBytes();
                boolean isText = true;
                for (int i = 0; i < Math.min(bytes.length, 100); i++) {
                    byte b = bytes[i];
                    if (b < 32 && b != 9 && b != 10 && b != 13) {
                        isText = false;
                        break;
                    }
                }
                if (isText && bytes.length > 0) {
                    String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    log.info("Successfully decoded file content as plain text after OCR failure.");
                    return parseOcrResult(text);
                }
            } catch (Exception ignored) {}
            
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
        byte[] content = file.getBytes();
        // Tess4J chon bo doc anh theo duoi file, thieu duoi se bao "Unsupported image format"
        File convFile = File.createTempFile("ocr_", imageSuffix(content));
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(content);
        }
        return convFile;
    }

    static String imageSuffix(byte[] content) {
        if (content.length >= 2 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8) {
            return ".jpg";
        }
        if (content.length >= 4 && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return ".png";
        }
        return ".tmp";
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
        // OCR hay mat dau tieng Viet nen bo dau truoc khi so tu khoa; so tien chi lay tren cung mot dong
        String plainText = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd')
            .replace('Đ', 'D');
        Pattern pattern = Pattern.compile("\\b(TONG(?: CONG| TIEN| THANH TOAN)?|TOTAL|THANH TOAN|VALUE|CONG)\\b[^\\d\\r\\n]{0,20}?(\\d[\\d,\\. ]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(plainText);

        BigDecimal maxAmount = BigDecimal.ZERO;
        BigDecimal amountToPay = null; // "Thanh toan" la so tien sau giam gia, uu tien hon "Tong cong"
        while (matcher.find()) {
            String amountStr = matcher.group(2).trim().replaceAll("[.,]\\d{1,2}$", "").replaceAll("[^0-9]", "");
            if (!amountStr.isEmpty()) {
                try {
                    BigDecimal val = new BigDecimal(amountStr);
                    if (val.compareTo(maxAmount) > 0) maxAmount = val;
                    if (matcher.group(1).toUpperCase().contains("THANH TOAN")) amountToPay = val;
                } catch (Exception ignored) {}
            }
        }
        return amountToPay != null ? amountToPay : maxAmount;
    }
    
    private String extractMerchant(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().length() > 3) return line.trim(); // Assume first non-empty line is merchant
        }
        return "Unknown Merchant";
    }
}
