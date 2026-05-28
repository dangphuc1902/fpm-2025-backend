package com.fpm2025.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {
    KafkaAutoConfiguration.class,
    RabbitAutoConfiguration.class,
    EurekaClientAutoConfiguration.class,
    ConfigClientAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.fpm2025.user_auth_service",
    "com.fpm_2025.wallet_service",
    "com.fpm2025.transaction_service",
    "com.fpm_2025.reportingservice",
    "com.fpm2025.notification_service",
    "com.fpm2025.ocr_service",
    "com.fpm2025.ai_service",
    "com.fpm2025.security"
})
@EnableAsync
public class MonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}
