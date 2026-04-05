package com.fpm2025.transaction_service.messaging;

import com.fpm2025.transaction_service.dto.BankNotificationRequest;
import com.fpm2025.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Lắng nghe sự kiện notification.parsed từ notification-service.
 * Thực hiện Auto-Reconcile: tự động tạo giao dịch trong transaction-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationParsedListener {

    private final TransactionService transactionService;

    @KafkaListener(topics = "notification.parsed", groupId = "transaction-group")
    public void handleNotificationParsed(@Payload Map<String, Object> event) {
        try {
            log.info("Kafka: Received notification.parsed event: {}", event);

            Long userId = getLong(event, "userId");
            if (userId == null) {
                log.warn("Kafka: Missing userId in alert, skipping.");
                return;
            }

            // Map Kafka event to DTO
            BankNotificationRequest request = BankNotificationRequest.builder()
                    .bankName((String) event.get("bankName"))
                    .amount(getBigDecimal(event, "amount"))
                    .type((String) event.get("type"))
                    .account((String) event.get("account"))
                    .note((String) event.get("note"))
                    .transactionRef((String) event.get("transactionRef"))
                    .balance((String) event.get("balance"))
                    .transactionTime((String) event.get("transactionTime"))
                    .build();

            log.info("Kafka: Auto-creating transaction for user: {} - Amount: {}", userId, request.getAmount());
            transactionService.processBankNotification(userId, request);
            
            log.info("✅ Kafka: Auto-reconcile completed for notificationId: {}", event.get("notificationId"));
        } catch (Exception e) {
            log.error("❌ Kafka: Error processing notification.parsed event: {}", e.getMessage(), e);
        }
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
