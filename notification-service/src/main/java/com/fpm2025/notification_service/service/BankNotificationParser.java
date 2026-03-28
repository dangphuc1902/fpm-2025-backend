package com.fpm2025.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser thông báo ngân hàng từ SMS hoặc push notification.
 *
 * Hỗ trợ:
 * - MB Bank (MBBank)
 * - Vietcombank (VCB)
 * - MoMo
 * - Techcombank (TCB)
 * - VPBank
 * - VietinBank
 * - Generic fallback
 */
@Slf4j
@Service
public class BankNotificationParser {

    // =========================================================================
    // ParseResult inner class
    // =========================================================================

    public record ParseResult(
            String bankName,
            BigDecimal amount,
            String type,           // INCOME | EXPENSE
            String account,        // Số TK / nguồn
            String note,           // Nội dung giao dịch
            boolean parsed
    ) {
        public static ParseResult failed(String bankName) {
            return new ParseResult(bankName, null, null, null, null, false);
        }
    }

    // =========================================================================
    // Detect bank from notification package or content
    // =========================================================================

    public String detectBank(String packageName, String rawContent) {
        if (packageName != null) {
            if (packageName.contains("mbbank") || packageName.contains("mb.bank")) return "MBBank";
            if (packageName.contains("vietcombank") || packageName.contains("vcb")) return "VCB";
            if (packageName.contains("momo")) return "MoMo";
            if (packageName.contains("techcombank") || packageName.contains("tcb")) return "Techcombank";
            if (packageName.contains("vpbank")) return "VPBank";
            if (packageName.contains("vietinbank")) return "VietinBank";
        }
        if (rawContent != null) {
            String upper = rawContent.toUpperCase();
            if (upper.contains("MBBANK") || upper.contains("MB BANK")) return "MBBank";
            if (upper.contains("VIETCOMBANK") || upper.contains("VCB")) return "VCB";
            if (upper.contains("MOMO") || upper.contains("VI MOMO")) return "MoMo";
            if (upper.contains("TECHCOMBANK") || upper.contains("TCB")) return "Techcombank";
            if (upper.contains("VPBANK")) return "VPBank";
            if (upper.contains("VIETINBANK") || upper.contains("CTG")) return "VietinBank";
        }
        return "Unknown";
    }

    // =========================================================================
    // Main parse entry point
    // =========================================================================

    public ParseResult parse(String bankName, String rawContent) {
        if (rawContent == null || rawContent.isBlank()) return ParseResult.failed(bankName);

        try {
            return switch (bankName) {
                case "MBBank"      -> parseMBBank(rawContent);
                case "VCB"         -> parseVCB(rawContent);
                case "MoMo"        -> parseMoMo(rawContent);
                case "Techcombank" -> parseTechcombank(rawContent);
                case "VPBank"      -> parseVPBank(rawContent);
                case "VietinBank"  -> parseVietinBank(rawContent);
                default            -> parseGeneric(bankName, rawContent);
            };
        } catch (Exception e) {
            log.warn("Parse failed for bank={}: {}", bankName, e.getMessage());
            return ParseResult.failed(bankName);
        }
    }

    // =========================================================================
    // MB Bank Parser
    // Ví dụ: "TK 12345678 - SD: 5,000,000 VND. Giao dich: -500,000 VND luc 10:30 01/01/2025. ND: Mua hang"
    // =========================================================================

    private ParseResult parseMBBank(String content) {
        // Pattern cho giao dịch debit (chi tiêu)
        Pattern debitPattern = Pattern.compile(
                "(?:TK|STK|Tai khoan)?\\s*[:\\-]?\\s*(?<account>[\\d*]+).*?" +
                "(?:GD|Giao dich|GD truc tuyen)?\\s*[:\\-]?\\s*(?<sign>[+-])?(?<amount>[\\d,\\.]+)\\s*(?:VND|USD|VNĐ).*?" +
                "(?:ND|Noi dung|Mo ta)?\\s*[:\\-]?\\s*(?<note>.+?)(?:\\.|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        // Simpler patterns
        Pattern amountPattern = Pattern.compile("([+-]?)([\\d,\\.]+)\\s*(?:VND|VNĐ|USD)", Pattern.CASE_INSENSITIVE);
        Pattern accountPattern = Pattern.compile("(?:TK|STK).*?([\\d*]{4,})", Pattern.CASE_INSENSITIVE);
        Pattern notePattern = Pattern.compile("(?:ND|Noi dung|Mo ta)[:\\s]+(.+?)(?:\\.|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher amountMatcher = amountPattern.matcher(content);
        BigDecimal amount = null;
        String type = "EXPENSE";

        if (amountMatcher.find()) {
            String sign = amountMatcher.group(1);
            String amtStr = amountMatcher.group(2).replaceAll("[,\\.](?=\\d{3})", "").replace(",", ".");
            try {
                amount = new BigDecimal(amtStr.replace(".", ""));
                if ("+".equals(sign) || content.toUpperCase().contains("CHUYEN KHOAN DEN")
                        || content.toUpperCase().contains("NHAN TIEN") || content.toUpperCase().contains("+" )) {
                    type = "INCOME";
                }
            } catch (NumberFormatException e) {
                log.debug("MBBank: Cannot parse amount from: {}", amtStr);
            }
        }

        String account = null;
        Matcher accMatcher = accountPattern.matcher(content);
        if (accMatcher.find()) account = accMatcher.group(1);

        String note = null;
        Matcher noteMatcher = notePattern.matcher(content);
        if (noteMatcher.find()) note = noteMatcher.group(1).trim();

        boolean parsed = amount != null;
        return new ParseResult("MBBank", amount, type, account, note, parsed);
    }

    // =========================================================================
    // Vietcombank (VCB) Parser
    // Ví dụ: "So du TK: 1234567890 la 10,000,000 VND sau GD: -500,000 VND vao 10:30 01/01/2025. Noi dung: Thanh toan dich vu"
    // =========================================================================

    private ParseResult parseVCB(String content) {
        Pattern amountPattern = Pattern.compile(
                "GD[:\\s]+([+-]?)([\\d,]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);
        Pattern accountPattern = Pattern.compile("TK[:\\s]+([\\d*]+)", Pattern.CASE_INSENSITIVE);
        Pattern notePattern = Pattern.compile("(?:Noi dung|ND)[:\\s]+([^.]+)", Pattern.CASE_INSENSITIVE);

        BigDecimal amount = null;
        String type = "EXPENSE";
        String account = null;
        String note = null;

        Matcher amt = amountPattern.matcher(content);
        if (amt.find()) {
            String sign = amt.group(1);
            try {
                amount = new BigDecimal(amt.group(2).replaceAll(",", ""));
                type = "+".equals(sign) ? "INCOME" : "EXPENSE";
            } catch (NumberFormatException ignore) {}
        }
        Matcher acc = accountPattern.matcher(content);
        if (acc.find()) account = acc.group(1);
        Matcher nte = notePattern.matcher(content);
        if (nte.find()) note = nte.group(1).trim();

        return new ParseResult("VCB", amount, type, account, note, amount != null);
    }

    // =========================================================================
    // MoMo Parser
    // Ví dụ: "Ban da chuyen 200,000 VND toi [Quan ca phe]. So du: 1,000,000 VND"
    // hoặc: "Ban da nhan 500,000 VND tu [Nguyen Van A]"
    // =========================================================================

    private ParseResult parseMoMo(String content) {
        Pattern sendPattern = Pattern.compile(
                "(?:chuyen|thanh toan|tra tien)\\s+([\\d,]+)\\s*(?:VND|VNĐ)\\s+(?:toi|cho|den)\\s+(.+?)(?:\\.|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Pattern receivePattern = Pattern.compile(
                "(?:nhan|duoc nhan)\\s+([\\d,]+)\\s*(?:VND|VNĐ)\\s+tu\\s+(.+?)(?:\\.|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher send = sendPattern.matcher(content);
        if (send.find()) {
            try {
                BigDecimal amount = new BigDecimal(send.group(1).replaceAll(",", ""));
                String note = "Chuyển đến: " + send.group(2).trim();
                return new ParseResult("MoMo", amount, "EXPENSE", "MoMo Wallet", note, true);
            } catch (NumberFormatException ignore) {}
        }

        Matcher recv = receivePattern.matcher(content);
        if (recv.find()) {
            try {
                BigDecimal amount = new BigDecimal(recv.group(1).replaceAll(",", ""));
                String note = "Nhận từ: " + recv.group(2).trim();
                return new ParseResult("MoMo", amount, "INCOME", "MoMo Wallet", note, true);
            } catch (NumberFormatException ignore) {}
        }

        return ParseResult.failed("MoMo");
    }

    // =========================================================================
    // Techcombank Parser
    // =========================================================================

    private ParseResult parseTechcombank(String content) {
        Pattern debitPattern  = Pattern.compile("DEBIT\\s+([\\d,]+)\\s*VND", Pattern.CASE_INSENSITIVE);
        Pattern creditPattern = Pattern.compile("CREDIT\\s+([\\d,]+)\\s*VND", Pattern.CASE_INSENSITIVE);
        Pattern accountPattern = Pattern.compile("Acct[:\\s]+([\\d*]+)", Pattern.CASE_INSENSITIVE);
        Pattern notePattern   = Pattern.compile("(?:Ref|Desc|Details?)[:\\s]+([^\\n.]+)", Pattern.CASE_INSENSITIVE);

        BigDecimal amount = null;
        String type = "EXPENSE";

        Matcher debit = debitPattern.matcher(content);
        Matcher credit = creditPattern.matcher(content);

        if (debit.find()) {
            try { amount = new BigDecimal(debit.group(1).replaceAll(",", "")); type = "EXPENSE"; } catch (NumberFormatException ignore) {}
        } else if (credit.find()) {
            try { amount = new BigDecimal(credit.group(1).replaceAll(",", "")); type = "INCOME"; } catch (NumberFormatException ignore) {}
        }

        String account = null;
        Matcher acc = accountPattern.matcher(content);
        if (acc.find()) account = acc.group(1);

        String note = null;
        Matcher nte = notePattern.matcher(content);
        if (nte.find()) note = nte.group(1).trim();

        return new ParseResult("Techcombank", amount, type, account, note, amount != null);
    }

    // =========================================================================
    // VPBank Parser
    // =========================================================================

    private ParseResult parseVPBank(String content) {
        return parseGeneric("VPBank", content);
    }

    // =========================================================================
    // VietinBank Parser
    // =========================================================================

    private ParseResult parseVietinBank(String content) {
        return parseGeneric("VietinBank", content);
    }

    // =========================================================================
    // Generic Fallback Parser
    // =========================================================================

    private ParseResult parseGeneric(String bankName, String content) {
        // Tìm số tiền bất kỳ dạng: 1,234,567 VND
        Pattern amtPattern = Pattern.compile("([+-]?)([\\d,\\.]+)\\s*(?:VND|VNĐ|USD|đ)", Pattern.CASE_INSENSITIVE);
        Matcher amt = amtPattern.matcher(content);

        if (amt.find()) {
            try {
                String amtStr = amt.group(2).replaceAll(",", "");
                BigDecimal amount = new BigDecimal(amtStr);
                String sign = amt.group(1);
                String type = "+".equals(sign) ? "INCOME" : "EXPENSE";

                // Heuristic: nếu nội dung chứa từ "nhận" / "cộng" -> INCOME
                String upper = content.toUpperCase();
                if (upper.contains("NHAN") || upper.contains("CONG TIEN") || upper.contains("CREDIT")) {
                    type = "INCOME";
                }
                return new ParseResult(bankName, amount, type, null, content.length() > 100 ? content.substring(0, 100) : content, true);
            } catch (NumberFormatException ignore) {}
        }

        return ParseResult.failed(bankName);
    }

    // =========================================================================
    // MD5 checksum để dedup
    // =========================================================================

    public String computeChecksum(String content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }
}
