package com.fpm_2025.wallet_service.event.consumer;

import com.fpm2025.domain.event.UserCreatedEvent;
import com.fpm_2025.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer lắng nghe sự kiện 'user.created' từ user-auth-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedConsumer {

    private final WalletService walletService;

    @KafkaListener(topics = "user.created", groupId = "${spring.kafka.consumer.group-id:user-auth-group}")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("[Kafka Received] UserCreatedEvent: userId={} email={}", 
                event.getUserId(), event.getEmail());

        try {
            // Tự động tạo ví mặc định (CASH) cho người dùng mới
            walletService.createDefaultWallet(event.getUserId());
            log.info("[Kafka Processed] Default wallet created successfully for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("[Kafka Failed] Failed to create default wallet for userId={}, error={}", 
                    event.getUserId(), e.getMessage(), e);
            // Có thể retry hoặc gửi vào DLQ tùy thiết kế
        }
    }
}
