package com.fpm2025.notification_service.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "notification.queue", durable = "true"),
            exchange = @Exchange(value = "notification.exchange", ignoreDeclarationExceptions = "true"),
            key = "notification.routing.key"
    ))
    public void handleNotificationMessage(String message) {
        log.info("--------------------------------------------------");
        log.info("✉ [EMAIL SIMULATION]: Dang gui Email toi User...");
        log.info("Noi dung: {}", message);
        log.info("[OK] Gui email hoan tat!");
        log.info("--------------------------------------------------");
    }
}
