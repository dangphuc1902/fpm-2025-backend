package com.fpm_2025.wallet_service.messaging;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fpm_2025.wallet_service.service.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lắng nghe sự kiện đăng ký người dùng mới từ user-auth-service.
 * Khi có user mới, tự động khởi tạo ví mặc định (Ví Tiền Mặt).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedListener {

    private final WalletService walletService;

    @KafkaListener(topics = "user.created", groupId = "wallet-group")
    public void handleUserCreated(Map<String, Object> event) {
        try {
            log.info("Kafka: Received user.created event: {}", event);
            
            // Lấy userId từ event (đảm bảo kiểu Long)
            Object userIdObj = event.get("userId");
            Long userId;
            
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else {
                userId = Long.parseLong(userIdObj.toString());
            }
            
            log.info("Kafka: Creating default wallet for userId: {}", userId);
            walletService.createDefaultWallet(userId);
            
            log.info(" Kafka: Successfully processed user.created for userId: {}", userId);
        } catch (Exception e) {
            log.error(" Kafka: Error processing user.created event: {}", e.getMessage(), e);
        }
    }
}
