package com.fpm2025.notification_service.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mau lay tu thong bao HDBank that (10/2026) do chu du an cung cap; so TK va ho ten da thay the.
 * HD_INCOME_SALARY dung lai theo dinh dang vi anh chup bi cat mat so tien.
 */
class BankNotificationParserTest {

    private final BankNotificationParser parser = new BankNotificationParser();

    // ---------- Bien dong so du HDBank: phai nhan dien ----------
    private static final String HD_EXPENSE =
            "15/10/2026 20:12:03 TK xxxx1234\n-2,788,146\nSD 0\nSD KHA DUNG 0\nNGUYEN VAN A chuyen tien";
    private static final String HD_INCOME_SALARY =
            "06/10/2026 16:12:24 TK xxxx1234\n+10,000,000\nSD 12,888,146\nSD KHA DUNG 12,888,146\nTT LUONG T09.2026";

    // ---------- Tin HDBank KHONG phai bien dong: phai loai ----------
    private static final String HD_PROMO =
            "MỜI BẠN MỚI TIỀN TẤN TỚI – Giao dịch ngoại tệ cùng HDBank. Chỉ với vài bước giới thiệu Khách hàng "
          + "giao dịch mua ngoại tệ tại HDBank, tất cả Cộng tác viên cá nhân sẽ có cơ hội nhận hoa hồng hấp dẫn "
          + "với mức thưởng không giới hạn. Chi trả hoa hồng hàng tuần. Tìm hiểu thêm tại: https://hdbank.com.vn/vi/personal/promotion";
    private static final String HD_MAINTENANCE =
            "Kính gửi Quý Khách hàng, HDBank sẽ thực hiện bảo trì và nâng cấp hệ thống Thẻ. "
          + "Thời gian thực hiện: Từ 00:00 đến 04:10 sáng ngày 24/10/2026 (Thứ Bảy). "
          + "Phạm vi dịch vụ ảnh hưởng: dịch vụ ATM, Thẻ. Vui lòng liên hệ Hotline 19006060 (24/7).";

    @Test
    void hdbank_expense_isParsed() {
        var r = parser.parse(parser.detectBank("vn.com.hdbank.mobile", HD_EXPENSE), HD_EXPENSE);
        assertTrue(r.parsed());
        assertEquals("HDBank", r.bankName());
        assertEquals(0, new BigDecimal("2788146").compareTo(r.amount()));
        assertEquals("EXPENSE", r.type());
        assertEquals("xxxx1234", r.account());
        assertEquals("NGUYEN VAN A chuyen tien", r.note());
        assertEquals("15/10/2026 20:12:03", r.transactionTime());
    }

    @Test
    void hdbank_income_isParsed() {
        var r = parser.parse("HDBank", HD_INCOME_SALARY);
        assertTrue(r.parsed());
        assertEquals(0, new BigDecimal("10000000").compareTo(r.amount()));
        assertEquals("INCOME", r.type());
        assertEquals("12,888,146", r.balance());
        assertEquals("TT LUONG T09.2026", r.note());
    }

    @Test
    void hdbank_singleLineNotification_isParsed() {
        // Mot so dien thoai gop thong bao thanh 1 dong
        String oneLine = HD_EXPENSE.replace('\n', ' ');
        var r = parser.parse("HDBank", oneLine);
        assertTrue(r.parsed());
        assertEquals(0, new BigDecimal("2788146").compareTo(r.amount()));
    }

    @Test
    void hdbank_promotionAndMaintenance_areRejected() {
        assertFalse(parser.parse("HDBank", HD_PROMO).parsed());
        assertFalse(parser.parse("HDBank", HD_MAINTENANCE).parsed());
        assertFalse(BankNotificationParser.looksLikeBalanceChange(BankNotificationParser.normalize(HD_PROMO)));
        assertFalse(BankNotificationParser.looksLikeBalanceChange(BankNotificationParser.normalize(HD_MAINTENANCE)));
    }

    @Test
    void promoWithAmount_isRejected_evenForBankWithLooseFallback() {
        // Truoc khi sua: mau VCB "(giam|tru|trich) + so + VND" bien tin nay thanh khoan chi 50.000
        String promo = "Giảm 50.000 VND cho đơn đầu tiên khi thanh toán QR";
        assertFalse(parser.parse("VCB", promo).parsed());
        assertFalse(parser.parse("Unknown", promo).parsed());
    }

    @Test
    void otp_isAlwaysRejected() {
        String otp = "Mã OTP của bạn là 123456 cho giao dịch 2.000.000 VND tại TK 0381xxxx1234";
        assertFalse(parser.parse("MBBank", otp).parsed());
    }

    @Test
    void mbbank_existingFormat_stillParsed() {
        String mb = "MBBANK: TK 0381****1234 GD: -500,000VND luc 10:30 01/01/2025. SD: 4,500,000VND. ND: Mua hang ABC. Ma GD: 123456789";
        var r = parser.parse("MBBank", mb);
        assertTrue(r.parsed());
        assertEquals(0, new BigDecimal("500000").compareTo(r.amount()));
        assertEquals("EXPENSE", r.type());
    }

    @Test
    void vcb_accentedNotification_isParsedAfterNormalize() {
        String vcb = "Số dư TK 0071000123456 giảm 1.500.000 VND lúc 14:20:30 15/02/2025. SD hiện tại: 8.500.000 VND";
        var r = parser.parse("VCB", vcb);
        assertTrue(r.parsed());
        assertEquals(0, new BigDecimal("1500000").compareTo(r.amount()));
        assertEquals("EXPENSE", r.type());
    }

    // ---------- MoMo: mau that 10/2026 (tieu de + noi dung), ten/so TK da thay the ----------
    private static final String MOMO_RECEIVE = "Nhận tiền từ NGUYEN VAN B\nSố tiền: 250.000 đ về ví MoMo. Vào Chat để xem và phản hồi nhanh nhé.";
    private static final String MOMO_TRANSFER = "Chuyển tiền thành công\nBạn đã chuyển thành công số tiền 500.000 đ đến NGUYEN VAN C (Techcombank), số tài khoản MS00Txxxxxxxx.";
    private static final String MOMO_PAY_SHOP = "Thanh toán HIGHLANDS COFFEE thành công\nBạn đã thực hiện thanh toán thành công cho HIGHLANDS COFFEE với số tiền 39.000đ. Nhấn để kiểm tra giao dịch ngay bạn nhé.";
    private static final String MOMO_PAY_FAILED = "Thanh toán Apple không thành công\nThanh toán tại Apple với số tiền 599.000đ không thành công. Trường hợp cần hỗ trợ bạn hãy gửi yêu cầu trợ giúp.";
    private static final String MOMO_TOPUP = "Nạp tiền thành công\nQuý Khách đã nạp thành công số tiền 645.000 đ từ Ngân hàng Vietinbank vào Ví MoMo.";
    private static final String MOMO_FUND_WITHDRAW = "Rút quỹ thành công\nNGUYEN VAN B đã rút 109.000 đ khỏi Tiêu dùng";
    private static final String MOMO_FUND_TRANSFER = "NGUYEN VAN B đã chuyển tiền bằng tiền Quỹ\nGiao dịch chuyển tiền 250.000 đ bằng quỹ Tiền của tôi thành công.";
    private static final String MOMO_SETUP = "Thiết lập thanh toán thành công cho Apple Services\nBạn đã thiết lập thanh toán bằng Ví MoMo thành công";

    @Test
    void momo_receive_isIncome() {
        var r = parser.parse(parser.detectBank("com.mservice.momo", MOMO_RECEIVE), MOMO_RECEIVE);
        assertTrue(r.parsed());
        assertEquals("MoMo", r.bankName());
        assertEquals(0, new BigDecimal("250000").compareTo(r.amount()));
        assertEquals("INCOME", r.type());
        assertEquals("Nhận từ: NGUYEN VAN B", r.note());
    }

    @Test
    void momo_transfer_isExpense() {
        var r = parser.parse("MoMo", MOMO_TRANSFER);
        assertTrue(r.parsed());
        assertEquals(0, new BigDecimal("500000").compareTo(r.amount()));
        assertEquals("EXPENSE", r.type());
        assertEquals("Chuyển đến: NGUYEN VAN C", r.note());
    }

    @Test
    void momo_shopPayment_isExpenseWithMerchant() {
        var r = parser.parse("MoMo", MOMO_PAY_SHOP);
        assertTrue(r.parsed());
        assertEquals(0, new BigDecimal("39000").compareTo(r.amount()));
        assertEquals("EXPENSE", r.type());
        assertEquals("HIGHLANDS COFFEE", r.note());
    }

    @Test
    void momo_failedTopupFundAndSetup_areIgnored() {
        assertFalse(parser.parse("MoMo", MOMO_PAY_FAILED).parsed());
        assertFalse(parser.parse("MoMo", MOMO_TOPUP).parsed());          // chuyen giua hu: xu ly khi sua TRANSFER
        assertFalse(parser.parse("MoMo", MOMO_FUND_WITHDRAW).parsed());
        assertFalse(parser.parse("MoMo", MOMO_FUND_TRANSFER).parsed());
        assertFalse(parser.parse("MoMo", MOMO_SETUP).parsed());
    }
}
