package com.fpm_2025.wallet_service.messaging;

import com.fpm2025.domain.event.UserCreatedEvent;
import com.fpm_2025.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * Kafka / Spring Event Consumer: Lắng nghe sự kiện 'user.created' từ user-auth-service.
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
    @EventListener
    @Async
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("[Local/Kafka] Received UserCreatedEvent: userId={} email={}",
                event.getUserId(), event.getEmail());

        try {
            walletService.createDefaultWallet(event.getUserId());
            log.info("[Local/Kafka] ✅ Default wallet created for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("[Local/Kafka] ❌ Failed to create default wallet for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
