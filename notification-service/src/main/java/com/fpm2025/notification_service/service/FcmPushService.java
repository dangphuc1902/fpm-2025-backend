package com.fpm2025.notification_service.service;

import com.fpm2025.notification_service.entity.FcmTokenEntity;
import com.fpm2025.notification_service.repository.FcmTokenRepository;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Firebase Cloud Messaging Push Service.
 *
 * Hai chế độ hoạt động:
 *   - PRODUCTION: FirebaseMessaging != null → gửi push thực qua FCM API
 *   - SIMULATION: FirebaseMessaging == null → log message (dev/test)
 *
 * Hỗ trợ:
 *   - Gửi đến 1 device (sendToDevice)
 *   - Gửi đến tất cả devices của user (sendToUser)
 *   - Multicast đến nhiều tokens (sendMulticast)
 */
@Service
@Slf4j
public class FcmPushService {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenRepository fcmTokenRepository;

    public FcmPushService(@Nullable FirebaseMessaging firebaseMessaging,
                          FcmTokenRepository fcmTokenRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.fcmTokenRepository = fcmTokenRepository;

        if (firebaseMessaging != null) {
            log.info("✅ FcmPushService initialized in PRODUCTION mode (real Firebase SDK).");
        } else {
            log.info("⚠️ FcmPushService initialized in SIMULATION mode (no Firebase SDK).");
        }
    }

    /**
     * Kiểm tra FCM có hoạt động thật hay simulation.
     */
    public boolean isProductionMode() {
        return firebaseMessaging != null;
    }

    // =========================================================================
    // Gửi push notification đến tất cả devices của user
    // =========================================================================

    public SendResult sendToUser(Long userId, String title, String body,
                                 String type, Map<String, String> data) {
        List<FcmTokenEntity> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);

        if (tokens.isEmpty()) {
            log.warn("No active FCM tokens for userId={}. Push not sent.", userId);
            return new SendResult(0, 0, "NO_TOKENS");
        }

        List<String> tokenStrings = tokens.stream()
                .map(FcmTokenEntity::getFcmToken)
                .collect(Collectors.toList());

        if (firebaseMessaging == null) {
            return simulatePush(userId, title, body, type, data, tokens);
        }

        return sendMulticast(userId, title, body, type, data, tokenStrings);
    }

    // =========================================================================
    // Gửi đến 1 device token cụ thể
    // =========================================================================

    public SendResult sendToDevice(String fcmToken, String title, String body,
                                   String type, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.info("[FCM-SIM] → token={}... | title={} | body={}", 
                    fcmToken.substring(0, Math.min(15, fcmToken.length())), title, body);
            return new SendResult(1, 0, "SIMULATED");
        }

        try {
            Message message = buildMessage(fcmToken, title, body, type, data);
            String messageId = firebaseMessaging.send(message);
            log.info("✅ FCM sent to token={}... messageId={}", 
                    fcmToken.substring(0, Math.min(15, fcmToken.length())), messageId);
            return new SendResult(1, 0, messageId);
        } catch (FirebaseMessagingException e) {
            log.error("❌ FCM send failed: code={}, msg={}", 
                    e.getMessagingErrorCode(), e.getMessage());
            handleFcmError(fcmToken, e);
            return new SendResult(0, 1, e.getMessagingErrorCode().name());
        }
    }

    // =========================================================================
    // Multicast: Gửi đến nhiều tokens cùng lúc (batch)
    // =========================================================================

    private SendResult sendMulticast(Long userId, String title, String body,
                                     String type, Map<String, String> data,
                                     List<String> tokens) {
        try {
            // Build data payload
            Map<String, String> fullData = new java.util.HashMap<>(data != null ? data : Map.of());
            fullData.put("type", type);

            MulticastMessage multicast = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(fullData)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setClickAction("OPEN_NOTIFICATION")
                                    .setSound("default")
                                    .setChannelId("fpm_notification")
                                    .build())
                            .build())
                    .addAllTokens(tokens)
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(multicast);

            int success = response.getSuccessCount();
            int failure = response.getFailureCount();

            log.info("✅ FCM multicast to userId={}: success={}, failure={}", 
                    userId, success, failure);

            // Handle failed tokens (unregister stale tokens)
            if (failure > 0) {
                handleMulticastFailures(response, tokens);
            }

            return new SendResult(success, failure, "MULTICAST_SENT");

        } catch (FirebaseMessagingException e) {
            log.error("❌ FCM multicast failed for userId={}: {}", userId, e.getMessage());
            return new SendResult(0, tokens.size(), e.getMessagingErrorCode().name());
        }
    }

    // =========================================================================
    // Build single FCM message
    // =========================================================================

    private Message buildMessage(String token, String title, String body,
                                 String type, Map<String, String> data) {
        Map<String, String> fullData = new java.util.HashMap<>(data != null ? data : Map.of());
        fullData.put("type", type);

        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(fullData)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setClickAction("OPEN_NOTIFICATION")
                                .setSound("default")
                                .setChannelId("fpm_notification")
                                .build())
                        .build())
                .build();
    }

    // =========================================================================
    // Error handling: deactivate stale tokens
    // =========================================================================

    private void handleMulticastFailures(BatchResponse response, List<String> tokens) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                FirebaseMessagingException ex = responses.get(i).getException();
                if (ex != null && isTokenInvalid(ex.getMessagingErrorCode())) {
                    String staleToken = tokens.get(i);
                    log.warn("Deactivating stale FCM token: {}...", 
                            staleToken.substring(0, Math.min(15, staleToken.length())));
                    fcmTokenRepository.findAll().stream()
                            .filter(t -> t.getFcmToken().equals(staleToken))
                            .findFirst()
                            .ifPresent(t -> {
                                t.setIsActive(false);
                                fcmTokenRepository.save(t);
                            });
                }
            }
        }
    }

    private void handleFcmError(String token, FirebaseMessagingException e) {
        if (isTokenInvalid(e.getMessagingErrorCode())) {
            log.warn("Token invalid, deactivating: {}...", 
                    token.substring(0, Math.min(15, token.length())));
            fcmTokenRepository.findAll().stream()
                    .filter(t -> t.getFcmToken().equals(token))
                    .findFirst()
                    .ifPresent(t -> {
                        t.setIsActive(false);
                        fcmTokenRepository.save(t);
                    });
        }
    }

    private boolean isTokenInvalid(MessagingErrorCode code) {
        return code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT;
    }

    // =========================================================================
    // Simulation mode (khi không có Firebase credentials)
    // =========================================================================

    private SendResult simulatePush(Long userId, String title, String body,
                                    String type, Map<String, String> data,
                                    List<FcmTokenEntity> tokens) {
        log.info("══════════════════════════════════════════════════════");
        log.info("📱 [FCM SIMULATION] Push to userId={}", userId);
        log.info("   Devices: {} active token(s)", tokens.size());
        for (FcmTokenEntity t : tokens) {
            log.info("   → {} ({}) token={}...", 
                    t.getDeviceId(), t.getDeviceType(),
                    t.getFcmToken().substring(0, Math.min(15, t.getFcmToken().length())));
        }
        log.info("   Title : {}", title);
        log.info("   Body  : {}", body);
        log.info("   Type  : {}", type);
        log.info("   Data  : {}", data);
        log.info("══════════════════════════════════════════════════════");

        return new SendResult(tokens.size(), 0, "SIMULATED");
    }

    // =========================================================================
    // Result DTO
    // =========================================================================

    public record SendResult(int successCount, int failureCount, String status) {
        public boolean hasSuccess() { return successCount > 0; }
    }
}
