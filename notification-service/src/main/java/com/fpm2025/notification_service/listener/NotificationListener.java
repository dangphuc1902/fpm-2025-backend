package com.fpm2025.notification_service.listener;

import com.fpm2025.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Listener nhận events từ RabbitMQ và Kafka.
 *
 * RabbitMQ: notification từ transaction-service / wallet-service
 * Kafka:
 *   - transaction.created: notify user có giao dịch mới
 *   - transaction.deleted: notify user đã xóa giao dịch
 *   - user.created: welcome notification
 *   - wallet.created: thông báo ví mới được tạo
 *   - balance.changed: thông báo biến động số dư ví
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    // =========================================================================
    // RabbitMQ: Notification queue (từ transaction-service, wallet-service)
    // =========================================================================

    @RabbitListener(bindings = @QueueBinding(
            value    = @Queue(value = "notification.queue", durable = "true"),
            exchange = @Exchange(value = "notification.exchange", ignoreDeclarationExceptions = "true"),
            key      = "notification.routing.key"
    ))
    public void handleNotificationMessage(String message) {
        log.info("==============================================");
        log.info("📨 [RabbitMQ] Received notification task");
        log.info("   Content: {}", message);
        log.info("==============================================");
    }

    // =========================================================================
    // Kafka: transaction.created → Thông báo giao dịch mới
    // =========================================================================

    @KafkaListener(topics = "transaction.created", groupId = "notification-service")
    public void handleTransactionCreated(@Payload Map<String, Object> event) {
        try {
            Long userId = getLong(event, "userId");
            if (userId == null) return;

            String type     = (String) event.getOrDefault("type", "EXPENSE");
            Object amount   = event.get("amount");
            String desc     = (String) event.getOrDefault("description", "");
            String currency = (String) event.getOrDefault("currency", "VND");

            String emoji = "INCOME".equals(type) ? "💰" : "💸";
            String title = emoji + " Giao dịch " + ("INCOME".equals(type) ? "thu nhập" : "chi tiêu");
            String body  = String.format("%s %s %s", amount, currency,
                    desc != null && !desc.isBlank() ? "- " + desc : "");

            notificationService.sendFcm(userId, title, body, "TRANSACTION", Map.of(
                    "type", type,
                    "amount", amount != null ? amount.toString() : "0"
            ));
            log.info("Kafka: Processed transaction.created for userId={}", userId);
        } catch (Exception e) {
            log.error("Kafka: Error handling transaction.created event", e);
        }
    }

    // =========================================================================
    // Kafka: transaction.deleted → Thông báo xóa giao dịch
    // =========================================================================

    @KafkaListener(topics = "transaction.deleted", groupId = "notification-service")
    public void handleTransactionDeleted(@Payload Map<String, Object> event) {
        try {
            Long userId = getLong(event, "userId");
            if (userId == null) return;

            Object txId   = event.get("transactionId");
            Object amount = event.get("amount");

            notificationService.sendFcm(userId,
                    "🗑️ Xóa giao dịch",
                    String.format("Giao dịch #%s (%s VND) đã được xóa", txId, amount),
                    "TRANSACTION",
                    Map.of("transactionId", txId != null ? txId.toString() : "0"));
        } catch (Exception e) {
            log.error("Kafka: Error handling transaction.deleted event", e);
        }
    }

    // =========================================================================
    // Kafka: user.created → Welcome notification
    // =========================================================================

    @KafkaListener(topics = "user.created", groupId = "notification-service")
    public void handleUserCreated(@Payload Map<String, Object> event) {
        try {
            Long userId  = getLong(event, "userId");
            String name  = (String) event.getOrDefault("username", "bạn");
            if (userId == null) return;

            notificationService.sendFcm(userId,
                    "🎉 Chào mừng đến FPM!",
                    String.format("Xin chào %s! Ví mặc định của bạn đã được tạo.", name),
                    "SYSTEM",
                    Map.of("event", "user.created"));
            log.info("Kafka: Welcome notification sent for userId={}", userId);
        } catch (Exception e) {
            log.error("Kafka: Error handling user.created event", e);
        }
    }

    // =========================================================================
    // Kafka: wallet.created → Thông báo ví mới
    // =========================================================================

    @KafkaListener(topics = "wallet.created", groupId = "notification-service")
    public void handleWalletCreated(@Payload Map<String, Object> event) {
        try {
            Long userId = getLong(event, "userId");
            if (userId == null) return;

            String walletName = (String) event.getOrDefault("walletName", "Ví mới");

            notificationService.sendFcm(userId,
                    "👛 Ví mới đã tạo",
                    String.format("Ví '%s' đã được tạo thành công.", walletName),
                    "SYSTEM",
                    Map.of("event", "wallet.created"));
            log.info("Kafka: Wallet created notification sent for userId={}", userId);
        } catch (Exception e) {
            log.error("Kafka: Error handling wallet.created event", e);
        }
    }

    // =========================================================================
    // Kafka: balance.changed → Thông báo biến động số dư
    // =========================================================================

    @KafkaListener(topics = "balance.changed", groupId = "notification-service")
    public void handleBalanceChanged(@Payload Map<String, Object> event) {
        try {
            Long userId = getLong(event, "userId");
            if (userId == null) return;

            Object newBalance = event.get("newBalance");
            Object change = event.get("changeAmount");

            notificationService.sendFcm(userId,
                    "💳 Biến động số dư ví",
                    String.format("Số dư thay đổi: %s VND. Số dư hiện tại: %s VND", change, newBalance),
                    "WALLET",
                    Map.of(
                            "event", "balance.changed",
                            "newBalance", newBalance != null ? newBalance.toString() : "0"
                    ));
        } catch (Exception e) {
            log.error("Kafka: Error handling balance.changed event", e);
        }
    }

    // =========================================================================
    // Kafka: budget.alerts → Cảnh báo ngân sách (80%, 100%)
    // =========================================================================

    @KafkaListener(topics = "budget.alerts", groupId = "notification-service")
    public void handleBudgetAlert(@Payload Map<String, Object> event) {
        try {
            Long userId = getLong(event, "userId");
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
            log.info("Kafka: Processed budget.alert for userId={}, threshold={}%", userId, threshold);
        } catch (Exception e) {
            log.error("Kafka: Error handling budget.alerts event", e);
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number num) return num.longValue();
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
