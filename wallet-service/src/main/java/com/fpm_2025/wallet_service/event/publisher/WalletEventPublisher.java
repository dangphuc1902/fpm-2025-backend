package com.fpm_2025.wallet_service.event.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fpm_2025.wallet_service.event.model.WalletCreatedEvent;

@Component
@RequiredArgsConstructor
public class WalletEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishWalletCreatedEvent(WalletCreatedEvent event) {
        kafkaTemplate.send("wallet.created", event);
    }
}