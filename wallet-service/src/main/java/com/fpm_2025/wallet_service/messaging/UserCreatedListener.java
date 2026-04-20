package com.fpm_2025.wallet_service.messaging;

import com.fpm2025.domain.event.UserCreatedEvent;
import com.fpm_2025.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer: Lắng nghe sự kiện 'user.created' từ user-auth-service.
 *
 * <p>Khi có user mới đăng ký (Email hoặc Google OAuth2), user-auth-service
 * publish {@link UserCreatedEvent} vào topic "user.created".
 * Consumer này nhận event và tự động khởi tạo ví mặc định (Ví Tiền Mặt / CASH)
 * cho user đó trong wallet-service.
 *
 * <p><b>Contract:</b>
 * <ul>
 *   <li>Topic: {@code user.created}</li>
 *   <li>Group ID: {@code wallet-group} (riêng biệt với user-auth-group)</li>
 *   <li>Payload: {@link UserCreatedEvent} — strongly-typed JSON</li>
 * </ul>
 *
 * <p><b>Idempotent:</b> {@code createDefaultWallet()} kiểm tra ví đã tồn tại
 * trước khi tạo mới, đảm bảo an toàn khi Kafka re-deliver message.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedListener {

    private final WalletService walletService;

    @KafkaListener(
            topics = "user.created",
            groupId = "wallet-group",
            containerFactory = "userCreatedKafkaListenerContainerFactory"
    )
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("[Kafka] Received UserCreatedEvent: userId={} email={}",
                event.getUserId(), event.getEmail());

        try {
            walletService.createDefaultWallet(event.getUserId());
            log.info("[Kafka] ✅ Default wallet created for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("[Kafka] ❌ Failed to create default wallet for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            // Non-blocking: log lỗi nhưng không re-throw để tránh loop retry vô hạn
            // TODO: Cân nhắc gửi vào Dead Letter Queue (DLQ) cho môi trường production
        }
    }
}
