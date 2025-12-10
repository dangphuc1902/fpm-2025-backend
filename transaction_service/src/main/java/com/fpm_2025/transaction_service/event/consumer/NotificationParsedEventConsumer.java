package com.fpm_2025.transaction_service.event.consumer;

//package com.fpm2025.transaction.service.event.consumer;

import com.fpm2025.domain.notification.event.NotificationParsedEvent;
import com.fpm2025.messaging.constant.KafkaTopics;
import com.fpm2025.messaging.kafka.consumer.BaseEventConsumer;
import com.fpm2025.transaction.service.service.NotificationSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationParsedEventConsumer extends BaseEventConsumer<NotificationParsedEvent> {

    private final NotificationSyncService notificationSyncService;

    @Override
    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_PARSED,
            groupId = "transaction-service-notification-group"
    )
    public void processEvent(NotificationParsedEvent event) {
        log.info("Processing notification parsed event: notificationId={}", event.getNotificationId());
        
        // Create transaction from notification
        notificationSyncService.createTransactionFromNotification(event);
    }

    @Override
    protected String getEventType() {
        return "NotificationParsed";
    }

    @Override
    protected Class<NotificationParsedEvent> getEventClass() {
        return NotificationParsedEvent.class;
    }

    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_PARSED,
            groupId = "transaction-service-notification-group"
    )
    public void consume(String message, Acknowledgment acknowledgment) {
        handleEvent(message, acknowledgment);
    }
}
