package com.fpm_2025.wallet_service.event.publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fpm2025.domain.event.WalletCreatedEvent;

@Component
public class WalletEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public WalletEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventPublisher = eventPublisher;
    }

    public void publishWalletCreatedEvent(WalletCreatedEvent event) {
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send("wallet.created", event);
            } catch (Exception e) {
                // Ignore or log
            }
        }
        eventPublisher.publishEvent(event);
    }
}