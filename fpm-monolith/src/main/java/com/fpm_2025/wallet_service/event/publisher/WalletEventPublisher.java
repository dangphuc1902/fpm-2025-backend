package com.fpm_2025.wallet_service.event.publisher;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.fpm2025.domain.event.WalletCreatedEvent;

@Component
public class WalletEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public WalletEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishWalletCreatedEvent(WalletCreatedEvent event) {
        eventPublisher.publishEvent(event);
    }
}