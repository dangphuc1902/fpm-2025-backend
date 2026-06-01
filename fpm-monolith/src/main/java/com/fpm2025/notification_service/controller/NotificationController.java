package com.fpm2025.notification_service.controller;

import com.fpm2025.notification_service.entity.BankNotificationEntity;
import com.fpm2025.notification_service.entity.FcmTokenEntity;
import com.fpm2025.notification_service.entity.NotificationHistoryEntity;
import com.fpm2025.notification_service.service.FcmPushService;
import com.fpm2025.notification_service.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * REST API cho notification-service.
 *
 * Endpoints:
 * POST  /api/v1/notifications/receive            — Nhận bank SMS notification
 * POST  /api/v1/notifications/fcm/register        — Đăng ký FCM token
 * GET   /api/v1/notifications/history             — Xem lịch sử
 * PATCH /api/v1/notifications/{id}/read           — Mark 1 notification as read
 * PATCH /api/v1/notifications/read-all            — Mark tất cả as read
 * GET   /api/v1/notifications/unread-count        — Số chưa đọc
 * GET   /api/v1/notifications/fcm/status          — Kiểm tra FCM mode (production/simulation)
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final FcmPushService fcmPushService;

    // =========================================================================
    // POST /api/v1/notifications/receive
    // Android app gửi bank SMS notification lên
    // =========================================================================

    @PostMapping("/receive")
    public ResponseEntity<?> receiveBankNotification(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody BankNotifRequest request) {

        // X-User-Id được inject bởi API Gateway sau khi validate token
        Long effectiveUserId = userId != null ? userId : request.userId();
        if (effectiveUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }

        log.info("POST /receive from userId={}, pkg={}", effectiveUserId, request.packageName());

        try {
            BankNotificationEntity result = notificationService.receiveBankNotification(
                    effectiveUserId,
                    request.packageName(),
                    request.rawContent()
            );

            if (result == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Duplicate notification, already processed"));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", result.getId(),
                    "bankName", result.getBankName(),
                    "isProcessed", result.getIsProcessed(),
                    "parsedAmount", result.getParsedAmount() != null ? result.getParsedAmount() : 0,
                    "parsedType", result.getParsedType() != null ? result.getParsedType() : "",
                    "parsedAccount", result.getParsedAccount() != null ? result.getParsedAccount() : "",
                    "parsedNote", result.getParsedNote() != null ? result.getParsedNote() : ""
            ));

        } catch (Exception e) {
            log.error("Error processing bank notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // POST /api/v1/notifications/fcm/register
    // Client gửi FCM token khi mở app hoặc token refresh
    // =========================================================================

    @PostMapping("/fcm/register")
    public ResponseEntity<?> registerFcmToken(
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @Valid @RequestBody FcmRegisterRequest request) {

        Long userId = userIdHeader != null ? userIdHeader : request.userId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }

        log.info("POST /fcm/register for userId={}, device={}", userId, request.deviceId());

        try {
            FcmTokenEntity token = notificationService.registerFcmToken(
                    userId, request.deviceId(), request.fcmToken(), request.deviceType());

            return ResponseEntity.ok(Map.of(
                    "id", token.getId(),
                    "userId", token.getUserId(),
                    "deviceId", token.getDeviceId(),
                    "deviceType", token.getDeviceType(),
                    "isActive", token.getIsActive(),
                    "message", "FCM token registered successfully"
            ));
        } catch (Exception e) {
            log.error("Error registering FCM token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // GET /api/v1/notifications/history
    // =========================================================================

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        log.info("GET /history userId={}, page={}, unreadOnly={}", userId, page, unreadOnly);

        Page<NotificationHistoryEntity> result = notificationService.getHistory(userId, page, size);

        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "currentPage", page,
                "unreadCount", notificationService.getUnreadCount(userId)
        ));
    }

    // =========================================================================
    // GET /api/v1/notifications/unread-count
    // =========================================================================

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "unreadCount", notificationService.getUnreadCount(userId)
        ));
    }

    // =========================================================================
    // PATCH /api/v1/notifications/{id}/read
    // =========================================================================

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    // =========================================================================
    // PATCH /api/v1/notifications/read-all
    // =========================================================================

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // =========================================================================
    // GET /api/v1/notifications/fcm/status
    // Kiểm tra FCM đang chạy mode nào
    // =========================================================================

    @GetMapping("/fcm/status")
    public ResponseEntity<?> getFcmStatus() {
        boolean isProduction = fcmPushService.isProductionMode();
        return ResponseEntity.ok(Map.of(
                "mode", isProduction ? "PRODUCTION" : "SIMULATION",
                "firebaseConnected", isProduction,
                "description", isProduction
                        ? "Firebase Admin SDK connected. Push notifications are sent via FCM."
                        : "Firebase disabled. Push notifications are logged only (simulation)."
        ));
    }

    // =========================================================================
    // Request Records
    // =========================================================================

    public record BankNotifRequest(
            Long userId,
            String packageName,
            @NotBlank String rawContent
    ) {}

    public record FcmRegisterRequest(
            Long userId,
            @NotBlank String deviceId,
            @NotBlank String fcmToken,
            String deviceType   // ANDROID | IOS | WEB
    ) {}
}
