package com.fpm2025.notification_service.service;

import com.fpm2025.domain.event.ParsedNotificationEvent;
import com.fpm2025.notification_service.entity.BankNotificationEntity;
import com.fpm2025.notification_service.entity.FcmTokenEntity;
import com.fpm2025.notification_service.entity.NotificationHistoryEntity;
import com.fpm2025.notification_service.repository.BankNotificationRepository;
import com.fpm2025.notification_service.repository.FcmTokenRepository;
import com.fpm2025.notification_service.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Core notification service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationHistoryRepository historyRepository;
    private final BankNotificationRepository bankNotifRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final BankNotificationParser parser;
    private final FcmPushService fcmPushService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PARSED_TOPIC = "notification.parsed";

    @Transactional
    public BankNotificationEntity receiveBankNotification(
            Long userId, String packageName, String rawContent) {

        log.info("Receiving bank notification from userId={}, pkg={}", userId, packageName);

        String checksum = parser.computeChecksum(rawContent);
        if (bankNotifRepository.existsByChecksum(checksum)) {
            log.info("Duplicate notification detected (checksum={}), skipping.", checksum);
            return bankNotifRepository.findByChecksum(checksum).orElse(null);
        }

        String bankName = parser.detectBank(packageName, rawContent);
        BankNotificationParser.ParseResult result = parser.parse(bankName, rawContent);

        BankNotificationEntity entity = BankNotificationEntity.builder()
                .userId(userId)
                .bankName(bankName)
                .rawContent(rawContent)
                .parsedAmount(result.amount())
                .parsedType(result.type())
                .parsedAccount(result.account())
                .parsedNote(result.note())
                .isProcessed(false)
                .checksum(checksum)
                .build();

        BankNotificationEntity saved = bankNotifRepository.save(entity);

        if (result.parsed()) {
            log.info("✅ Parsed successfully: bank={}, amount={}, type={}, account={}, ref={}",
                    bankName, result.amount(), result.type(), result.account(), result.transactionRef());

            publishParsedEvent(userId, saved, result);

            String title = "INCOME".equals(result.type())
                    ? "💰 Nhận tiền từ " + bankName
                    : "💸 Chi tiêu qua " + bankName;
            String body = String.format("Giao dịch: %,.0f VND - %s",
                    result.amount() != null ? result.amount().doubleValue() : 0,
                    result.note() != null ? result.note() : "");
            sendFcm(userId, title, body, "TRANSACTION", Map.of(
                    "bankNotifId", String.valueOf(saved.getId()),
                    "amount", result.amount() != null ? result.amount().toPlainString() : "0",
                    "type", result.type() != null ? result.type() : "",
                    "transactionRef", result.transactionRef() != null ? result.transactionRef() : ""));

            saved.setIsProcessed(true);
            saved.setProcessedAt(LocalDateTime.now());
            bankNotifRepository.save(saved);
        } else {
            log.warn("⚠️ Could not parse bank notification from {}: {}",
                    bankName, rawContent.substring(0, Math.min(80, rawContent.length())));
            sendFcm(userId, "📩 Thông báo mới từ " + bankName,
                    "Không thể tự động nhận diện giao dịch. Bấm để xem.",
                    "SYSTEM", Map.of());
        }

        return saved;
    }

    @Transactional
    public FcmTokenEntity registerFcmToken(Long userId, String deviceId,
                                           String fcmToken, String deviceType) {
        log.info("Registering FCM token for userId={}, device={}", userId, deviceId);

        FcmTokenEntity token = fcmTokenRepository
                .findByUserIdAndDeviceId(userId, deviceId)
                .map(existing -> {
                    existing.setFcmToken(fcmToken);
                    existing.setDeviceType(deviceType != null ? deviceType : "ANDROID");
                    existing.setIsActive(true);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return existing;
                })
                .orElse(FcmTokenEntity.builder()
                        .userId(userId)
                        .deviceId(deviceId)
                        .fcmToken(fcmToken)
                        .deviceType(deviceType != null ? deviceType : "ANDROID")
                        .isActive(true)
                        .build());

        return fcmTokenRepository.save(token);
    }

    public Page<NotificationHistoryEntity> getHistory(Long userId, int page, int size) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public long getUnreadCount(Long userId) {
        return historyRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        int count = historyRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read for userId={}", count, userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        historyRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId) && !n.getIsRead()) {
                n.setIsRead(true);
                n.setReadAt(LocalDateTime.now());
                historyRepository.save(n);
            }
        });
    }

    public void sendFcm(Long userId, String title, String body,
                        String type, Map<String, String> data) {

        FcmPushService.SendResult result = fcmPushService.sendToUser(userId, title, body, type, data);

        String status;
        if ("NO_TOKENS".equals(result.status())) {
            status = "FAILED";
        } else if (result.hasSuccess()) {
            status = fcmPushService.isProductionMode() ? "SENT" : "SIMULATED";
        } else {
            status = "FAILED";
        }

        NotificationHistoryEntity history = NotificationHistoryEntity.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .type(type)
                .payloadJson(data != null && !data.isEmpty() ? data.toString() : null)
                .isRead(false)
                .sentVia("FCM")
                .status(status)
                .build();

        historyRepository.save(history);
    }

    private void publishParsedEvent(Long userId, BankNotificationEntity saved,
                                    BankNotificationParser.ParseResult result) {
        try {
            ParsedNotificationEvent event = ParsedNotificationEvent.builder()
                    .eventType("NOTIFICATION_PARSED")
                    .notificationId(saved.getId())
                    .userId(userId)
                    .bankName(saved.getBankName())
                    .amount(result.amount())
                    .type(result.type())
                    .account(result.account())
                    .note(result.note())
                    .transactionRef(result.transactionRef())
                    .balance(result.balance())
                    .transactionTime(result.transactionTime())
                    .parsedAt(LocalDateTime.now().toString())
                    .build();

            kafkaTemplate.send(PARSED_TOPIC, String.valueOf(userId), event)
                    .whenComplete((sendResult, ex) -> {
                        if (ex == null) {
                            log.info("✅ Kafka: Published [{}] for userId={}, notifId={}, amount={}, partition={}",
                                    PARSED_TOPIC, userId, saved.getId(), result.amount(),
                                    sendResult.getRecordMetadata().partition());
                        } else {
                            log.error("❌ Kafka: Failed to publish [{}] for userId={}: {}",
                                    PARSED_TOPIC, userId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("❌ Kafka: Exception publishing [{}]: {}", PARSED_TOPIC, e.getMessage(), e);
        }
    }
}
