package com.fpm2025.notification_service.listener;

import com.fpm2025.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

/**
 * Listener nhận events từ Spring ApplicationEventPublisher.
 *
 * Đồng bộ các sự kiện chi tiêu, nạp tiền, tạo tài khoản và biến động ngân sách
 * một cách bất đồng bộ trong JVM thông qua @EventListener và @Async.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    // =========================================================================
    // Spring Event Listener for Transaction Created → Thông báo giao dịch mới
    // =========================================================================

    @EventListener
    @Async
    public void handleTransactionCreatedEvent(com.fpm2025.domain.event.TransactionCreatedEvent event) {
        try {
            Long userId = event.getUserId();
            String type = event.getType();
            java.math.BigDecimal amount = event.getAmount();
            String desc = event.getNote();

            String emoji = "INCOME".equals(type) ? "💰" : "💸";
            String title = emoji + " Giao dịch " + ("INCOME".equals(type) ? "thu nhập" : "chi tiêu");
            String body  = String.format("%s VND %s", amount, desc != null && !desc.isBlank() ? "- " + desc : "");

            notificationService.sendFcm(userId, title, body, "TRANSACTION", Map.of(
                    "type", type != null ? type : "EXPENSE",
                    "amount", amount != null ? amount.toString() : "0"
            ));
            log.info("SpringEvent: Processed TransactionCreatedEvent for userId={}", userId);
        } catch (Exception e) {
            log.error("SpringEvent: Error handling TransactionCreatedEvent", e);
        }
    }

    // =========================================================================
    // Spring Event Listener for User Created → Welcome notification
    // =========================================================================

    @EventListener
    @Async
    public void handleUserCreatedEvent(com.fpm2025.domain.event.UserCreatedEvent event) {
        try {
            Long userId = event.getUserId();
            String name = event.getEmail();

            notificationService.sendFcm(userId,
                    "🎉 Chào mừng đến FPM!",
                    String.format("Xin chào %s! Ví mặc định của bạn đã được tạo.", name),
                    "SYSTEM",
                    Map.of("event", "user.created"));
            log.info("SpringEvent: Welcome notification sent for userId={}", userId);
        } catch (Exception e) {
            log.error("SpringEvent: Error handling UserCreatedEvent", e);
        }
    }

    // =========================================================================
    // Spring Event Listener for Wallet Created → Thông báo ví mới
    // =========================================================================

    @EventListener
    @Async
    public void handleWalletCreatedEvent(com.fpm2025.domain.event.WalletCreatedEvent event) {
        try {
            Long userId = event.getUserId();
            String walletName = event.getName();

            notificationService.sendFcm(userId,
                    "👛 Ví mới đã tạo",
                    String.format("Ví '%s' đã được tạo thành công.", walletName),
                    "SYSTEM",
                    Map.of("event", "wallet.created"));
            log.info("SpringEvent: Wallet created notification sent for userId={}", userId);
        } catch (Exception e) {
            log.error("SpringEvent: Error handling WalletCreatedEvent", e);
        }
    }

    // =========================================================================
    // Spring Event Listener for Transaction Deleted → Thông báo xóa giao dịch
    // =========================================================================

    @EventListener(condition = "#event instanceof T(java.util.Map) and #event.get('topic') == 'transaction.deleted'")
    @Async
    @SuppressWarnings("unchecked")
    public void handleTransactionDeletedSpring(java.util.Map event) {
        try {
            Long userId = getLong((Map<String, Object>) event, "userId");
            if (userId == null) return;

            Object txId = event.get("transactionId");
            Object amount = event.get("amount");

            notificationService.sendFcm(userId,
                    "🗑️ Xóa giao dịch",
                    String.format("Giao dịch #%s (%s VND) đã được xóa", txId, amount),
                    "TRANSACTION",
                    Map.of("transactionId", txId != null ? txId.toString() : "0"));
            log.info("SpringEvent: Processed transaction.deleted for userId={}", userId);
        } catch (Exception e) {
            log.error("SpringEvent: Error handling transaction.deleted", e);
        }
    }

    // =========================================================================
    // Spring Event Listener for Budget Alert → Cảnh báo ngân sách
    // =========================================================================

    @EventListener(condition = "#event instanceof T(java.util.Map) and #event.get('topic') == 'budget.alerts'")
    @Async
    @SuppressWarnings("unchecked")
    public void handleBudgetAlertSpring(java.util.Map event) {
        try {
            Long userId = getLong((Map<String, Object>) event, "userId");
            if (userId == null) return;

            String categoryName = (String) event.getOrDefault("categoryName", "Danh mục");
            Number threshold   = (Number) event.getOrDefault("thresholdPercent", 0);
            Object amountUsed  = event.get("amountUsed");
            Object amountLimit = event.get("amountLimit");

            String emoji = threshold.intValue() >= 100 ? "🚨" : "⚠️";
            String title = emoji + " Cảnh báo ngân sách " + categoryName;
            
            String body;
            if (threshold.intValue() >= 100) {
                body = String.format("Bạn đã vượt quá hạn mức chi tiêu (%s/%s)!", amountUsed, amountLimit);
            } else {
                body = String.format("Bạn đã sử dụng %d%% hạn mức chi tiêu cho %s (%s/%s).", 
                        threshold.intValue(), categoryName, amountUsed, amountLimit);
            }

            notificationService.sendFcm(userId, title, body, "BUDGET", Map.of(
                    "budgetId", event.getOrDefault("budgetId", "0").toString(),
                    "threshold", threshold.toString()
            ));
            log.info("SpringEvent: Processed budget.alert for userId={}, threshold={}%", userId, threshold);
        } catch (Exception e) {
            log.error("SpringEvent: Error handling budget.alerts", e);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number num) return num.longValue();
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
