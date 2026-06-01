package com.fpm2025.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser thông báo ngân hàng từ SMS / push notification.
 *
 * Hỗ trợ parse sâu cho:
 *   - MB Bank (MBBank) — nhiều format SMS thực tế
 *   - Vietcombank (VCB) — format chuẩn VCB
 *   - MoMo — chuyển/nhận/thanh toán
 *   - Techcombank, VPBank, VietinBank (cơ bản)
 *   - Generic fallback
 *
 * Mỗi parser hỗ trợ nhiều pattern regex để cover các biến thể SMS thực tế.
 */
@Slf4j
@Service
public class BankNotificationParser {

    // =========================================================================
    // ParseResult
    // =========================================================================

    public record ParseResult(
            String bankName,
            BigDecimal amount,
            String type,           // INCOME | EXPENSE
            String account,        // Số TK / nguồn
            String balance,        // Số dư sau GD (nếu có)
            String transactionRef, // Mã giao dịch
            String note,           // Nội dung giao dịch
            String transactionTime,// Thời gian GD
            boolean parsed
    ) {
        public static ParseResult failed(String bankName) {
            return new ParseResult(bankName, null, null, null, null, null, null, null, false);
        }

        /** Backward-compatible constructor (không có balance, ref, time) */
        public ParseResult(String bankName, BigDecimal amount, String type,
                           String account, String note, boolean parsed) {
            this(bankName, amount, type, account, null, null, note, null, parsed);
        }
    }

    // =========================================================================
    // Detect bank from notification package / content
    // =========================================================================

    public String detectBank(String packageName, String rawContent) {
        if (packageName != null) {
            String pkg = packageName.toLowerCase();
            if (pkg.contains("mbbank") || pkg.contains("mb.bank") || pkg.contains("com.mbmobile")) return "MBBank";
            if (pkg.contains("vietcombank") || pkg.contains("vcb") || pkg.contains("com.VCB")) return "VCB";
            if (pkg.contains("momo") || pkg.contains("com.mservice")) return "MoMo";
            if (pkg.contains("techcombank") || pkg.contains("tcb")) return "Techcombank";
            if (pkg.contains("vpbank")) return "VPBank";
            if (pkg.contains("vietinbank") || pkg.contains("com.vietinbank")) return "VietinBank";
            if (pkg.contains("agribank") || pkg.contains("com.agribank")) return "Agribank";
            if (pkg.contains("bidv")) return "BIDV";
            if (pkg.contains("tpbank")) return "TPBank";
            if (pkg.contains("sacombank")) return "Sacombank";
        }
        if (rawContent != null) {
            String upper = rawContent.toUpperCase();
            if (upper.contains("MBBANK") || upper.contains("MB BANK") || upper.contains("NGAN HANG QUAN DOI")) return "MBBank";
            if (upper.contains("VIETCOMBANK") || upper.contains("VCB")) return "VCB";
            if (upper.contains("MOMO") || upper.contains("VI MOMO") || upper.contains("MSERVICE")) return "MoMo";
            if (upper.contains("TECHCOMBANK") || upper.contains("TCB")) return "Techcombank";
            if (upper.contains("VPBANK")) return "VPBank";
            if (upper.contains("VIETINBANK") || upper.contains("CTG")) return "VietinBank";
            if (upper.contains("AGRIBANK")) return "Agribank";
            if (upper.contains("BIDV")) return "BIDV";
            if (upper.contains("TPBANK")) return "TPBank";
            if (upper.contains("SACOMBANK")) return "Sacombank";
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
                case "BIDV"        -> parseBIDV(rawContent);
                default            -> parseGeneric(bankName, rawContent);
            };
        } catch (Exception e) {
            log.warn("Parse failed for bank={}: {}", bankName, e.getMessage());
            return ParseResult.failed(bankName);
        }
    }

    // =========================================================================
    // MB Bank Parser — Nhiều format SMS thực tế
    //
    // Format 1 (Debit):
    //   "MBBANK: TK 0381****1234 GD: -500,000VND luc 10:30 01/01/2025.
    //    SD: 4,500,000VND. ND: Mua hang ABC. Ma GD: 123456789"
    //
    // Format 2 (Credit):
    //   "MBBANK: TK 0381****1234 +1,000,000VND luc 14:20 15/02/2025.
    //    SD: 5,500,000VND. ND: NGUYEN VAN A chuyen tien. Ma GD: 987654321"
    //
    // Format 3 (Biến động số dư):
    //   "MBBANK: So du TK 0381xxxx1234 -200,000VND
    //    vao luc 08:30 ngay 01/03/2025. SD hien tai: 3,800,000VND.
    //    Noi dung: Thanh toan hoa don dien"
    //
    // Format 4 (App push notification):
    //   "Tai khoan 123456789 duoc ghi no 500.000 VND.
    //    So du kha dung: 4.500.000 VND. Noi dung: ABC XYZ"
    // =========================================================================

    private ParseResult parseMBBank(String content) {
        BigDecimal amount = null;
        String type = "EXPENSE";
        String account = null;
        String balance = null;
        String transactionRef = null;
        String note = null;
        String time = null;

        // ── Pattern 1: Standard SMS "GD: -500,000VND" hoặc "+1,000,000VND"
        Pattern gdPattern = Pattern.compile(
                "(?:GD|Giao dich)\\s*:\\s*([+-]?)([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);

        // ── Pattern 2: Biến động "TK ... -500,000VND" (số tiền trực tiếp sau TK)
        Pattern bdPattern = Pattern.compile(
                "(?:TK|So du TK)\\s+[\\dxX*]+\\s+([+-])([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);

        // ── Pattern 3: "duoc ghi no/ghi co 500.000 VND"
        Pattern ghiPattern = Pattern.compile(
                "(?:duoc\\s+)?ghi\\s+(no|co)\\s+([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);

        // ── Pattern 4: Generic amount near bank keywords
        Pattern genericAmtPattern = Pattern.compile(
                "([+-]?)([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);

        // Try patterns in order of specificity
        Matcher m;

        // Pattern 1: GD: -500,000VND
        m = gdPattern.matcher(content);
        if (m.find()) {
            amount = parseAmount(m.group(2));
            type = determineType(m.group(1), content);
        }

        // Pattern 2: TK xxxx +/-amount
        if (amount == null) {
            m = bdPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(2));
                type = "+".equals(m.group(1)) ? "INCOME" : "EXPENSE";
            }
        }

        // Pattern 3: ghi nợ / ghi có
        if (amount == null) {
            m = ghiPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(2));
                type = "co".equalsIgnoreCase(m.group(1)) ? "INCOME" : "EXPENSE";
            }
        }

        // Pattern 4: Fallback generic amount
        if (amount == null) {
            m = genericAmtPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(2));
                type = determineType(m.group(1), content);
            }
        }

        // ── Tài khoản
        Pattern accPattern = Pattern.compile(
                "(?:TK|STK|Tai khoan|So du TK)\\s*:?\\s*([\\dxX*]{4,})",
                Pattern.CASE_INSENSITIVE);
        m = accPattern.matcher(content);
        if (m.find()) account = m.group(1);

        // ── Số dư
        Pattern balPattern = Pattern.compile(
                "(?:SD|So du|SD hien tai|So du kha dung)\\s*:?\\s*([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);
        m = balPattern.matcher(content);
        if (m.find()) balance = m.group(1);

        // ── Mã giao dịch
        Pattern refPattern = Pattern.compile(
                "(?:Ma GD|Ma giao dich|Ref)\\s*:?\\s*([A-Za-z0-9]+)",
                Pattern.CASE_INSENSITIVE);
        m = refPattern.matcher(content);
        if (m.find()) transactionRef = m.group(1);

        // ── Nội dung
        Pattern notePattern = Pattern.compile(
                "(?:ND|Noi dung|Mo ta)\\s*:?\\s*(.+?)(?:\\.\\s*(?:Ma|SD|$)|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        m = notePattern.matcher(content);
        if (m.find()) note = m.group(1).trim();

        // ── Thời gian
        Pattern timePattern = Pattern.compile(
                "(?:luc|vao luc|vao)\\s+(\\d{1,2}:\\d{2}\\s*(?:ngay\\s*)?\\d{1,2}/\\d{1,2}/\\d{2,4})",
                Pattern.CASE_INSENSITIVE);
        m = timePattern.matcher(content);
        if (m.find()) time = m.group(1).trim();

        boolean parsed = amount != null;
        log.debug("MBBank parse: amount={}, type={}, account={}, balance={}, ref={}, note={}, time={}, parsed={}",
                amount, type, account, balance, transactionRef, note, time, parsed);

        return new ParseResult("MBBank", amount, type, account, balance, transactionRef, note, time, parsed);
    }

    // =========================================================================
    // Vietcombank (VCB) Parser
    //
    // Format 1 (SMS biến động số dư):
    //   "VCB: TK 1234567890 so du -500,000VND luc 10:30 01/01/2025.
    //    SD: 4,500,000VND. Ref: xxxxxx. ND: Thanh toan dich vu"
    //
    // Format 2 (Smart OTP / App):
    //   "So du TK 0071000xxxxx giam 1,500,000 VND luc 14:20:30 15/02/2025.
    //    SD hien tai: 8,500,000 VND. GD: chuyen tien online"
    //
    // Format 3 (Nhận tiền):
    //   "VCB: TK 1234567890 so du +2,000,000VND luc 10:00 01/03/2025.
    //    Ref: 123ABC. ND: NGUYEN VAN A chuyen tien"
    //
    // Format 4 (Internet Banking):
    //   "SD TK 0071001234567 +500,000VND vao 15:30 20/01/2025.
    //    SD: 6,000,000VND. ND: Nhan luong T1/2025"
    // =========================================================================

    private ParseResult parseVCB(String content) {
        BigDecimal amount = null;
        String type = "EXPENSE";
        String account = null;
        String balance = null;
        String transactionRef = null;
        String note = null;
        String time = null;

        // ── Pattern 1: "so du -500,000VND" hoặc "+2,000,000VND"
        Pattern sdPattern = Pattern.compile(
                "(?:so du|SD TK)\\s*(?:[\\dxX*]+\\s+)?([+-]?)([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);

        // ── Pattern 2: "GD: -500,000 VND" hoặc "GD: +500,000 VND"
        Pattern gdPattern = Pattern.compile(
                "GD\\s*:\\s*([+-]?)([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);

        // ── Pattern 3: "giam 1,500,000 VND" hoặc "tang 500,000 VND"
        Pattern giamTangPattern = Pattern.compile(
                "(?:giam|tru|trich)\\s+([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);
        Pattern tangPattern = Pattern.compile(
                "(?:tang|cong|nhan duoc)\\s+([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);

        Matcher m;

        // Try GD pattern first (most specific for VCB)
        m = gdPattern.matcher(content);
        if (m.find()) {
            amount = parseAmount(m.group(2));
            type = "+".equals(m.group(1)) ? "INCOME" : "EXPENSE";
        }

        // Then try "so du +/- amount"
        if (amount == null) {
            m = sdPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(2));
                type = "+".equals(m.group(1)) ? "INCOME" : "EXPENSE";
            }
        }

        // Try "giam / tang"
        if (amount == null) {
            m = giamTangPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(1));
                type = "EXPENSE";
            } else {
                m = tangPattern.matcher(content);
                if (m.find()) {
                    amount = parseAmount(m.group(1));
                    type = "INCOME";
                }
            }
        }

        // Fallback: generic amount
        if (amount == null) {
            Pattern genAmt = Pattern.compile("([+-]?)([\\d,.]+)\\s*(?:VND|VNĐ)", Pattern.CASE_INSENSITIVE);
            m = genAmt.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(2));
                type = determineType(m.group(1), content);
            }
        }

        // ── Tài khoản
        Pattern accPattern = Pattern.compile(
                "(?:TK|STK|SD TK)\\s*:?\\s*([\\dxX*]{6,})",
                Pattern.CASE_INSENSITIVE);
        m = accPattern.matcher(content);
        if (m.find()) account = m.group(1);

        // ── Số dư
        Pattern balPattern = Pattern.compile(
                "(?:SD|So du|SD hien tai)\\s*:?\\s*([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);
        m = balPattern.matcher(content);
        if (m.find()) balance = m.group(1);

        // ── Ref (mã giao dịch VCB thường dạng "Ref: xxx")
        Pattern refPattern = Pattern.compile(
                "(?:Ref|Ma GD)\\s*:?\\s*([A-Za-z0-9.]+)",
                Pattern.CASE_INSENSITIVE);
        m = refPattern.matcher(content);
        if (m.find()) transactionRef = m.group(1);

        // ── Nội dung
        Pattern notePattern = Pattern.compile(
                "(?:ND|Noi dung|GD)\\s*:\\s*(.+?)(?:\\.\\s*$|\\.\\s*(?:Ref|Ma|SD)|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        m = notePattern.matcher(content);
        if (m.find()) note = m.group(1).trim();

        // ── Thời gian
        Pattern timePattern = Pattern.compile(
                "luc\\s+(\\d{1,2}:\\d{2}(?::\\d{2})?\\s*\\d{1,2}/\\d{1,2}/\\d{2,4})",
                Pattern.CASE_INSENSITIVE);
        m = timePattern.matcher(content);
        if (m.find()) time = m.group(1).trim();

        boolean parsed = amount != null;
        log.debug("VCB parse: amount={}, type={}, account={}, balance={}, ref={}, note={}, parsed={}",
                amount, type, account, balance, transactionRef, note, parsed);

        return new ParseResult("VCB", amount, type, account, balance, transactionRef, note, time, parsed);
    }

    // =========================================================================
    // MoMo Parser
    //
    // Format 1 (Chuyển tiền):
    //   "Ban da chuyen thanh cong 200,000d toi NGUYEN VAN A. Ma GD: 123456789.
    //    So du vi: 800,000d"
    //
    // Format 2 (Nhận tiền):
    //   "Ban da nhan 500,000d tu TRAN VAN B. Ma GD: 987654321.
    //    So du vi: 1,300,000d"
    //
    // Format 3 (Thanh toán):
    //   "Thanh toan thanh cong 150,000d cho Grab. Ma GD: 111222333.
    //    So du vi: 650,000d"
    //
    // Format 4 (Rút tiền):
    //   "Rut tien thanh cong 1,000,000d ve TK ngan hang. Ma GD: 444555666"
    //
    // Format 5 (Nạp tiền):
    //   "Nap tien thanh cong 500,000d tu TK VCB. Ma GD: 777888999"
    //
    // Format 6 (QR / Thanh toán cửa hàng):
    //   "Ban da thanh toan 75,000d tai CUA HANG ABC qua QR.
    //    Ma GD: 121212. SD: 575,000d"
    // =========================================================================

    private ParseResult parseMoMo(String content) {
        BigDecimal amount = null;
        String type = null;
        String account = "MoMo Wallet";
        String balance = null;
        String transactionRef = null;
        String note = null;

        // ── EXPENSE patterns
        // Chuyển tiền
        Pattern sendPattern = Pattern.compile(
                "(?:chuyen|chuyen thanh cong|chuyen tien)\\s+(?:thanh cong\\s+)?([\\d,.]+)\\s*(?:VND|VNĐ|d|đ)\\s+(?:toi|cho|den)\\s+(.+?)(?:\\.|Ma|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        // Thanh toán
        Pattern payPattern = Pattern.compile(
                "(?:thanh toan|thanh toan thanh cong|TT)\\s+(?:thanh cong\\s+)?([\\d,.]+)\\s*(?:VND|VNĐ|d|đ)\\s+(?:cho|tai|toi)\\s+(.+?)(?:\\.|Ma|qua|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        // Rút tiền
        Pattern withdrawPattern = Pattern.compile(
                "(?:rut tien|rut)\\s+(?:thanh cong\\s+)?([\\d,.]+)\\s*(?:VND|VNĐ|d|đ)\\s+(?:ve|ra)\\s+(.+?)(?:\\.|Ma|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        // ── INCOME patterns
        // Nhận tiền
        Pattern receivePattern = Pattern.compile(
                "(?:nhan|nhan duoc|da nhan)\\s+([\\d,.]+)\\s*(?:VND|VNĐ|d|đ)\\s+(?:tu|from)\\s+(.+?)(?:\\.|Ma|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        // Nạp tiền
        Pattern topupPattern = Pattern.compile(
                "(?:nap tien|nap)\\s+(?:thanh cong\\s+)?([\\d,.]+)\\s*(?:VND|VNĐ|d|đ)\\s+(?:tu|from)\\s+(.+?)(?:\\.|Ma|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        // Hoàn tiền
        Pattern refundPattern = Pattern.compile(
                "(?:hoan tien|hoan)\\s+(?:thanh cong\\s+)?([\\d,.]+)\\s*(?:VND|VNĐ|d|đ)",
                Pattern.CASE_INSENSITIVE);

        Matcher m;

        // Try expense patterns
        m = sendPattern.matcher(content);
        if (m.find()) {
            amount = parseAmount(m.group(1));
            type = "EXPENSE";
            note = "Chuyển đến: " + m.group(2).trim();
        }

        if (amount == null) {
            m = payPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(1));
                type = "EXPENSE";
                note = "Thanh toán: " + m.group(2).trim();
            }
        }

        if (amount == null) {
            m = withdrawPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(1));
                type = "EXPENSE";
                note = "Rút tiền: " + m.group(2).trim();
            }
        }

        // Try income patterns
        if (amount == null) {
            m = receivePattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(1));
                type = "INCOME";
                note = "Nhận từ: " + m.group(2).trim();
            }
        }

        if (amount == null) {
            m = topupPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(1));
                type = "INCOME";
                note = "Nạp tiền từ: " + m.group(2).trim();
            }
        }

        if (amount == null) {
            m = refundPattern.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(1));
                type = "INCOME";
                note = "Hoàn tiền";
            }
        }

        // Fallback: generic
        if (amount == null) {
            Pattern genAmt = Pattern.compile("([\\d,.]+)\\s*(?:VND|VNĐ|d|đ)", Pattern.CASE_INSENSITIVE);
            m = genAmt.matcher(content);
            if (m.find()) {
                amount = parseAmount(m.group(1));
                type = determineType("", content);
                note = content.length() > 100 ? content.substring(0, 100) : content;
            }
        }

        // ── Mã giao dịch
        Pattern refPattern = Pattern.compile(
                "(?:Ma GD|Ma giao dich)\\s*:?\\s*([A-Za-z0-9]+)",
                Pattern.CASE_INSENSITIVE);
        m = refPattern.matcher(content);
        if (m.find()) transactionRef = m.group(1);

        // ── Số dư
        Pattern balPattern = Pattern.compile(
                "(?:So du vi|So du|SD)\\s*:?\\s*([\\d,.]+)\\s*(?:VND|VNĐ|d|đ)",
                Pattern.CASE_INSENSITIVE);
        m = balPattern.matcher(content);
        if (m.find()) balance = m.group(1);

        boolean parsed = amount != null && type != null;
        log.debug("MoMo parse: amount={}, type={}, ref={}, note={}, balance={}, parsed={}",
                amount, type, transactionRef, note, balance, parsed);

        return new ParseResult("MoMo", amount, type, account, balance, transactionRef, note, null, parsed);
    }

    // =========================================================================
    // Techcombank Parser
    // =========================================================================

    private ParseResult parseTechcombank(String content) {
        Pattern debitPattern  = Pattern.compile(
                "(?:DEBIT|tru|ghi no|trich)\\s+([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);
        Pattern creditPattern = Pattern.compile(
                "(?:CREDIT|cong|ghi co|nhan)\\s+([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);
        Pattern accountPattern = Pattern.compile(
                "(?:Acct|TK|STK)\\s*:?\\s*([\\dxX*]+)", Pattern.CASE_INSENSITIVE);
        Pattern notePattern = Pattern.compile(
                "(?:Ref|Desc|Details?|ND|Noi dung)\\s*:?\\s*([^\\n.]+)",
                Pattern.CASE_INSENSITIVE);
        Pattern balPattern = Pattern.compile(
                "(?:Bal|SD|So du)\\s*:?\\s*([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);

        BigDecimal amount = null;
        String type = "EXPENSE";
        String account = null, note = null, balance = null;
        Matcher m;

        m = debitPattern.matcher(content);
        if (m.find()) { amount = parseAmount(m.group(1)); type = "EXPENSE"; }

        if (amount == null) {
            m = creditPattern.matcher(content);
            if (m.find()) { amount = parseAmount(m.group(1)); type = "INCOME"; }
        }

        if (amount == null) {
            Pattern genAmt = Pattern.compile("([+-]?)([\\d,.]+)\\s*(?:VND|VNĐ)", Pattern.CASE_INSENSITIVE);
            m = genAmt.matcher(content);
            if (m.find()) { amount = parseAmount(m.group(2)); type = determineType(m.group(1), content); }
        }

        m = accountPattern.matcher(content);
        if (m.find()) account = m.group(1);
        m = notePattern.matcher(content);
        if (m.find()) note = m.group(1).trim();
        m = balPattern.matcher(content);
        if (m.find()) balance = m.group(1);

        return new ParseResult("Techcombank", amount, type, account, balance, null, note, null, amount != null);
    }

    // =========================================================================
    // VPBank Parser
    // =========================================================================

    private ParseResult parseVPBank(String content) {
        return parseGenericEnhanced("VPBank", content);
    }

    // =========================================================================
    // VietinBank Parser
    // =========================================================================

    private ParseResult parseVietinBank(String content) {
        return parseGenericEnhanced("VietinBank", content);
    }

    // =========================================================================
    // BIDV Parser
    // =========================================================================

    private ParseResult parseBIDV(String content) {
        return parseGenericEnhanced("BIDV", content);
    }

    // =========================================================================
    // Generic Enhanced Parser (dùng cho banks chưa có parser riêng)
    // =========================================================================

    private ParseResult parseGenericEnhanced(String bankName, String content) {
        BigDecimal amount = null;
        String type = "EXPENSE";
        String account = null, note = null, balance = null, transactionRef = null;
        Matcher m;

        // ── Amount
        Pattern amtPattern = Pattern.compile(
                "([+-]?)([\\d,.]+)\\s*(?:VND|VNĐ|USD|đ|d)",
                Pattern.CASE_INSENSITIVE);
        m = amtPattern.matcher(content);
        if (m.find()) {
            amount = parseAmount(m.group(2));
            type = determineType(m.group(1), content);
        }

        // ── Account
        Pattern accPattern = Pattern.compile(
                "(?:TK|STK|Tai khoan|Acct)\\s*:?\\s*([\\dxX*]{4,})",
                Pattern.CASE_INSENSITIVE);
        m = accPattern.matcher(content);
        if (m.find()) account = m.group(1);

        // ── Balance
        Pattern balPattern = Pattern.compile(
                "(?:SD|So du|Bal|SD hien tai)\\s*:?\\s*([\\d,.]+)\\s*(?:VND|VNĐ)",
                Pattern.CASE_INSENSITIVE);
        m = balPattern.matcher(content);
        if (m.find()) balance = m.group(1);

        // ── Note
        Pattern notePattern = Pattern.compile(
                "(?:ND|Noi dung|Mo ta|Desc|Ref)\\s*:?\\s*(.+?)(?:\\.|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        m = notePattern.matcher(content);
        if (m.find()) note = m.group(1).trim();
        else if (content.length() <= 150) note = content;

        // ── Transaction Ref
        Pattern refPattern = Pattern.compile(
                "(?:Ma GD|Ma giao dich|Ref No)\\s*:?\\s*([A-Za-z0-9]+)",
                Pattern.CASE_INSENSITIVE);
        m = refPattern.matcher(content);
        if (m.find()) transactionRef = m.group(1);

        return new ParseResult(bankName, amount, type, account, balance, transactionRef, note, null, amount != null);
    }

    // =========================================================================
    // Legacy Generic Parser (backward compatibility)
    // =========================================================================

    private ParseResult parseGeneric(String bankName, String content) {
        ParseResult enhanced = parseGenericEnhanced(bankName, content);
        return enhanced;
    }

    // =========================================================================
    // Utility: Parse Vietnamese amount format
    //
    // Các format số tiền phổ biến:
    //   "500,000"     → 500000
    //   "1.500.000"   → 1500000
    //   "500000"      → 500000
    //   "1,500,000.5" → 1500000  (bỏ phần thập phân VND)
    // =========================================================================

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            // Detect format: nếu dấu chấm đứng trước dấu phẩy cuối cùng → format quốc tế (1,500.00)
            // Nếu dấu phẩy đứng trước dấu chấm cuối cùng → format VN (1.500,00) hoặc (1.500.000)
            String cleaned = raw.trim();

            // Trường hợp đặc biệt: "1.500.000" (VN format dùng dấu chấm phân cách)
            // Regex: nếu có >=2 dấu chấm → chắc chắn dấu chấm là thousand separator
            long dotCount = cleaned.chars().filter(c -> c == '.').count();
            long commaCount = cleaned.chars().filter(c -> c == ',').count();

            if (dotCount >= 2) {
                // "1.500.000" → "1500000"
                cleaned = cleaned.replace(".", "");
            } else if (commaCount >= 2) {
                // "1,500,000" → "1500000"
                cleaned = cleaned.replace(",", "");
            } else if (dotCount == 1 && commaCount == 0) {
                // "500.000" → could be 500000 (VN) or 500.000 (decimal)
                // Heuristic: nếu sau dấu chấm có 3 digits → thousand separator
                int dotIdx = cleaned.indexOf('.');
                if (dotIdx >= 0 && cleaned.length() - dotIdx - 1 == 3) {
                    cleaned = cleaned.replace(".", ""); // Vietnamese format
                }
                // else keep as decimal
            } else if (commaCount == 1 && dotCount == 0) {
                // "500,000" → 500000:  dấu phẩy thousand separator
                int commaIdx = cleaned.indexOf(',');
                if (commaIdx >= 0 && cleaned.length() - commaIdx - 1 == 3) {
                    cleaned = cleaned.replace(",", "");
                } else {
                    // "500,50" → 500.50 (decimal)
                    cleaned = cleaned.replace(",", ".");
                }
            } else if (commaCount >= 1 && dotCount >= 1) {
                // "1,500.00" hoặc "1.500,00"
                int lastComma = cleaned.lastIndexOf(',');
                int lastDot = cleaned.lastIndexOf('.');
                if (lastComma > lastDot) {
                    // "1.500,00" → VN format, dấu chấm = thousand, dấu phẩy = decimal
                    cleaned = cleaned.replace(".", "").replace(",", ".");
                } else {
                    // "1,500.00" → international format
                    cleaned = cleaned.replace(",", "");
                }
            }

            BigDecimal result = new BigDecimal(cleaned);
            // VND không có phần thập phân, làm tròn
            if (result.scale() > 0 && result.compareTo(BigDecimal.valueOf(1000)) > 0) {
                result = result.setScale(0, java.math.RoundingMode.HALF_UP);
            }
            return result.compareTo(BigDecimal.ZERO) > 0 ? result : null;
        } catch (NumberFormatException e) {
            log.debug("Cannot parse amount from: '{}'", raw);
            return null;
        }
    }

    // =========================================================================
    // Utility: Determine INCOME / EXPENSE from sign + content heuristics
    // =========================================================================

    private String determineType(String sign, String content) {
        if ("+".equals(sign)) return "INCOME";
        if ("-".equals(sign)) return "EXPENSE";

        String upper = content.toUpperCase();
        // INCOME keywords
        if (upper.contains("NHAN TIEN") || upper.contains("NHAN DUOC") ||
            upper.contains("CHUYEN KHOAN DEN") || upper.contains("CONG TIEN") ||
            upper.contains("GHI CO") || upper.contains("CREDIT") ||
            upper.contains("TANG") || upper.contains("NAP TIEN") ||
            upper.contains("HOAN TIEN") || upper.contains("NHAN LUONG") ||
            upper.contains("LUONG THANG")) {
            return "INCOME";
        }
        // Default to EXPENSE
        return "EXPENSE";
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
